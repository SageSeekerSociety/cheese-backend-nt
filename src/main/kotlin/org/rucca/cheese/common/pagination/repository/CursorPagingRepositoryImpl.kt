package org.rucca.cheese.common.pagination.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Root
import java.io.Serializable
import org.rucca.cheese.common.pagination.model.Cursor
import org.rucca.cheese.common.pagination.model.CursorPage
import org.rucca.cheese.common.pagination.model.CursorPageInfo
import org.rucca.cheese.common.pagination.spec.CursorSpecification
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository

/**
 * Default implementation of CursorPagingRepository.
 *
 * This implementation handles cursor-based pagination, including cursor extraction, bidirectional
 * navigation, and efficient database queries.
 *
 * @param T The entity type
 * @param ID The entity ID type
 */
class CursorPagingRepositoryImpl<T, ID>(
    entityInformation: JpaEntityInformation<T, ID>,
    entityManager: EntityManager,
) :
    SimpleJpaRepository<T, ID>(entityInformation, entityManager),
    CursorPagingRepository<T, ID> where ID : Serializable, ID : Comparable<ID> {

    @PersistenceContext private val em: EntityManager = entityManager

    override fun <C : Cursor<T>> findAllWithCursor(
        cursorSpec: CursorSpecification<T, C>,
        cursor: C?,
        pageSize: Int,
    ): CursorPage<T, C> {
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(domainClass)
        val root = cq.from(domainClass)

        // Apply base conditions
        val predicate = cursorSpec.toPredicate(root, cq, cb)

        // Apply cursor conditions
        val cursorPredicate = cursorSpec.toCursorPredicate(root, cq, cb, cursor)

        // Combine all conditions
        if (cursorPredicate != null) {
            cq.where(cb.and(predicate, cursorPredicate))
        } else {
            cq.where(predicate)
        }

        // Apply sorting
        cq.orderBy(toJpaOrders(cursorSpec.getSort(), root, cb))

        // Create query with one extra result to check for next page
        val query = em.createQuery(cq).apply { maxResults = pageSize + 1 }

        // Execute query
        val results = query.resultList

        // Check if there's a next page
        val hasNext = results.size > pageSize
        val content = if (hasNext) results.subList(0, pageSize) else results

        // Build page info
        val pageInfo =
            CursorPageInfo(
                cursor =
                    if (content.isNotEmpty()) cursorSpec.extractCursor(content.first()) else null,
                pageSize = content.size,
                hasNext = hasNext,
                nextCursor =
                    if (hasNext && content.isNotEmpty()) cursorSpec.extractCursor(results[pageSize])
                    else null,
            )

        return CursorPage(content, pageInfo)
    }

    /** Convert Spring Data Sort to JPA Order list. */
    private fun toJpaOrders(
        sort: Sort,
        root: Root<T>,
        cb: CriteriaBuilder,
    ): List<jakarta.persistence.criteria.Order> {
        return sort
            .map { order ->
                if (order.isAscending) {
                    cb.asc(root.get<Any>(order.property))
                } else {
                    cb.desc(root.get<Any>(order.property))
                }
            }
            .toList()
    }
}
