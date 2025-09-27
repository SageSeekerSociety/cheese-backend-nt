package org.rucca.cheese.common.pagination.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.io.Serializable
import kotlin.math.nextTowards
import org.rucca.cheese.common.pagination.model.Cursor
import org.rucca.cheese.common.pagination.model.CursorPage
import org.rucca.cheese.common.pagination.model.CursorPageInfo
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor
import org.rucca.cheese.common.pagination.spec.CursorProjectionSupport
import org.rucca.cheese.common.pagination.spec.CursorSpecification
import org.rucca.cheese.common.pagination.util.CursorQueryUtils
import org.rucca.cheese.common.pagination.util.JpaUtils
import org.rucca.cheese.common.persistent.spec.ParadeCursorSpecification
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository

/**
 * Default implementation of CursorPagingRepository. Handles cursor-based pagination using Criteria
 * API and supports property paths.
 */
class CursorPagingRepositoryImpl<T, ID>(
    entityInformation: JpaEntityInformation<T, ID>,
    entityManager: EntityManager,
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), CursorPagingRepository<T, ID>
    where ID : Serializable, ID : Comparable<ID> { // Ensure ID is Comparable if used in cursor

    @PersistenceContext private val em: EntityManager = entityManager

    override fun <C : Cursor<T>> findAllWithCursor(
        cursorSpec: CursorSpecification<T, C>,
        cursor: C?,
        pageSize: Int,
    ): CursorPage<T, C> {
        require(pageSize > 0) { "Page size must be positive" }

        val cb = em.criteriaBuilder

        val projectionSupport = cursorSpec as? CursorProjectionSupport<*, *>
        return if (projectionSupport != null) {
            @Suppress("UNCHECKED_CAST")
            executeTupleQuery(
                cb,
                cursorSpec,
                projectionSupport as CursorProjectionSupport<T, C>,
                cursor,
                pageSize,
            )
        } else {
            executeEntityQuery(cb, cursorSpec, cursor, pageSize)
        }
    }

    private fun <C : Cursor<T>> executeTupleQuery(
        cb: CriteriaBuilder,
        cursorSpec: CursorSpecification<T, C>,
        projectionSupport: CursorProjectionSupport<T, C>,
        cursor: C?,
        pageSize: Int,
    ): CursorPage<T, C> {
        // === 第一臂：主查询 (score < :s) ===
        val cq: CriteriaQuery<Tuple> = cb.createTupleQuery()
        val root = cq.from(domainClass)

        val basePredicate = cursorSpec.toPredicate(root, cq, cb)
        val cursorPredicate = cursorSpec.toCursorPredicate(root, cq, cb, cursor)
        val finalPredicate = cursorPredicate?.let { cb.and(basePredicate, it) } ?: basePredicate
        cq.where(finalPredicate)

        // 为第一个查询构建投影
        val projection = projectionSupport.buildProjection(root, cq, cb)
        CursorQueryUtils.applyProjection(cq, root, projection.additionalSelections)
        val orders = projectionSupport.toJpaOrders(root, cb)
        if (orders.isNotEmpty()) {
            cq.orderBy(orders)
        }

        val query = em.createQuery(cq).apply { maxResults = pageSize + 1 }
        val mainArmTuples = executeQuery(query)

        var allTuples = mainArmTuples

        // === 第二臂：同分补齐查询 (当第一臂结果不足且有游标时触发) ===
        if (mainArmTuples.size <= pageSize && cursor != null) {
            val typedCursor = cursor as? TypedCompositeCursor<T>
            val paradeSpec = cursorSpec as? ParadeCursorSpecification<T, ID>

            if (typedCursor != null && paradeSpec != null) {
                val cursorScoreString =
                    typedCursor.values[paradeSpec.scoreAlias]?.unwrap() as? String
                val cursorIdValue = typedCursor.values[paradeSpec.idProperty.name]?.unwrap() as? ID

                if (cursorScoreString != null && cursorIdValue != null) {
                    val cursorScore = cursorScoreString.toDouble()

                    val cqTieBreaker: CriteriaQuery<Tuple> = cb.createTupleQuery()
                    val rootTieBreaker = cqTieBreaker.from(domainClass)

                    val basePredicateTieBreaker =
                        cursorSpec.toPredicate(rootTieBreaker, cqTieBreaker, cb)
                    val scoreExpr = paradeSpec.scoreExpression(rootTieBreaker, cb)
                    val scorePredicate = approximatelyEqual(scoreExpr, cursorScore, cb)
                    val idPredicate =
                        cb.greaterThan(
                            rootTieBreaker.get(paradeSpec.idProperty.name),
                            cursorIdValue,
                        )
                    cqTieBreaker.where(cb.and(basePredicateTieBreaker, scorePredicate, idPredicate))

                    // ================== 关键修复 START ==================
                    // 为第二条查询重新构建投影，获取绑定到新 root 的 Selection 和 Order
                    val projectionTieBreaker =
                        projectionSupport.buildProjection(rootTieBreaker, cqTieBreaker, cb)
                    CursorQueryUtils.applyProjection(
                        cqTieBreaker,
                        rootTieBreaker,
                        projectionTieBreaker.additionalSelections,
                    )
                    val ordersTieBreaker = projectionSupport.toJpaOrders(rootTieBreaker, cb)
                    if (ordersTieBreaker.isNotEmpty()) {
                        cqTieBreaker.orderBy(ordersTieBreaker)
                    }
                    // ================== 关键修复 END ==================

                    val tieBreakerQuery =
                        em.createQuery(cqTieBreaker).apply { maxResults = pageSize + 1 }
                    val tieBreakerTuples = executeQuery(tieBreakerQuery)

                    // 合并结果时要去重，因为可能有重叠
                    allTuples =
                        (mainArmTuples + tieBreakerTuples).distinctBy { it.get(0, domainClass) }
                }
            }
        }

        // === 处理合并后的结果 ===
        val hasNext = allTuples.size > pageSize
        val pageTuples = if (hasNext) allTuples.subList(0, pageSize) else allTuples

        val content = pageTuples.map(projection.entityExtractor)
        val firstCursor = pageTuples.firstOrNull()?.let(projection.cursorExtractor)
        val endCursor = pageTuples.lastOrNull()?.let(projection.cursorExtractor)
        val nextCursor = if (hasNext) endCursor else null

        val pageInfo =
            CursorPageInfo(
                cursor = firstCursor,
                pageSize = content.size,
                hasNext = hasNext,
                nextCursor = nextCursor,
            )

        return CursorPage(content, pageInfo)
    }

    private fun approximatelyEqual(
        expr: Expression<Double>,
        target: Double,
        cb: CriteriaBuilder,
    ): Predicate {
        val lowerBound = target.nextTowards(Double.NEGATIVE_INFINITY)
        val upperBound = target.nextTowards(Double.POSITIVE_INFINITY)
        return cb.between(expr, lowerBound, upperBound)
    }

    private fun <C : Cursor<T>> executeEntityQuery(
        cb: CriteriaBuilder,
        cursorSpec: CursorSpecification<T, C>,
        cursor: C?,
        pageSize: Int,
    ): CursorPage<T, C> {
        val cq = cb.createQuery(domainClass)
        val root = cq.from(domainClass)

        val predicate = cursorSpec.toPredicate(root, cq, cb)
        val cursorPredicate = cursorSpec.toCursorPredicate(root, cq, cb, cursor)
        val finalPredicate = cursorPredicate?.let { cb.and(predicate, it) } ?: predicate
        cq.where(finalPredicate)

        cq.orderBy(toJpaOrders(cursorSpec.getSort(), root, cb))

        val query = em.createQuery(cq).apply { maxResults = pageSize + 1 }

        val results = executeQuery(query)
        val hasNext = results.size > pageSize
        val content = if (hasNext) results.subList(0, pageSize) else results

        val firstEntityCursor = content.firstOrNull()?.let(cursorSpec::extractCursor)
        val endEntityCursor = content.lastOrNull()?.let(cursorSpec::extractCursor)
        val nextEntityCursor = if (hasNext) endEntityCursor else null

        val pageInfo =
            CursorPageInfo(
                cursor = firstEntityCursor,
                pageSize = content.size,
                hasNext = hasNext,
                nextCursor = nextEntityCursor,
            )

        return CursorPage(content, pageInfo)
    }

    private fun <R> executeQuery(query: jakarta.persistence.TypedQuery<R>): List<R> {
        return try {
            query.resultList
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute cursor query", e)
        }
    }

    /** Convert Spring Data Sort to JPA Order list, handling property paths. */
    private fun toJpaOrders(
        sort: Sort,
        root: Root<T>,
        cb: CriteriaBuilder,
    ): List<Order> { // Return type is jakarta.persistence.criteria.Order
        return sort
            .map { order ->
                // Use helper to get Path based on potentially nested property string
                val path = JpaUtils.getPath<Any>(root, order.property) // Use helper
                if (order.isAscending) {
                    cb.asc(path)
                } else {
                    cb.desc(path)
                }
            }
            .toList()
    }
}
