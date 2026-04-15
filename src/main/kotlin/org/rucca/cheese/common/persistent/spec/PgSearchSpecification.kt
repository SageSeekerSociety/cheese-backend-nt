package org.rucca.cheese.common.persistent.spec

import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Selection
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor
import org.rucca.cheese.common.pagination.spec.CursorProjection
import org.rucca.cheese.common.pagination.spec.CursorProjectionSupport
import org.rucca.cheese.common.pagination.spec.CursorSpecification
import org.rucca.cheese.common.query.internal.pagination.RelevanceCursorSupport
import org.rucca.cheese.common.query.internal.pagination.doubleValue
import org.rucca.cheese.common.query.internal.search.ParadeSearchConverter
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

// =====================================================================================
// Spring Data JPA integration helpers for Parade search
// =====================================================================================

fun <T> paradeSpecification(query: SearchQuery, idProperty: KProperty1<T, *>): Specification<T> =
    ParadeSearchConverter.specification(query, idProperty)

fun <T> Specification<T>.withParadeOrder(
    idProperty: KProperty1<T, *>,
    direction: Sort.Direction = Sort.Direction.DESC,
): Specification<T> {
    return this.and { root, criteriaQuery, criteriaBuilder ->
        criteriaQuery?.let { query ->
            if (
                query.resultType != Long::class.java &&
                    query.resultType != Long::class.javaObjectType
            ) {
                val scoreExpression =
                    criteriaBuilder.function(
                        "pdb_score",
                        Number::class.java,
                        root.get<Any>(idProperty.name),
                    )
                val order: Order =
                    if (direction.isAscending) {
                        criteriaBuilder.asc(scoreExpression)
                    } else {
                        criteriaBuilder.desc(scoreExpression)
                    }
                query.orderBy(order)
            }
        }
        criteriaBuilder.conjunction()
    }
}

fun <T : Any, ID : Comparable<ID>> paradeCursorSpecification(
    domainClass: Class<T>,
    idProperty: KProperty1<T, ID?>,
    searchQuery: SearchQuery,
    additionalSpec: Specification<T>? = null,
    direction: Sort.Direction = Sort.Direction.DESC,
    scoreAlias: String = ParadeCursorSpecification.SCORE_FIELD,
): ParadeCursorSpecification<T, ID> {
    val searchSpec = paradeSpecification<T>(searchQuery, idProperty)
    val combined = additionalSpec?.let { searchSpec.and(it) } ?: searchSpec
    return ParadeCursorSpecification<T, ID>(
        domainClass = domainClass,
        idPropertyRef = idProperty,
        baseSpec = combined,
        direction = direction,
        scoreAliasValue = scoreAlias,
    )
}

class ParadeCursorSpecification<T : Any, ID : Comparable<ID>>(
    private val domainClass: Class<T>,
    private val idPropertyRef: KProperty1<T, ID?>,
    private val baseSpec: Specification<T>,
    private val direction: Sort.Direction = Sort.Direction.DESC,
    private val scoreAliasValue: String = SCORE_FIELD,
) :
    CursorSpecification<T, TypedCompositeCursor<T>>,
    CursorProjectionSupport<T, TypedCompositeCursor<T>>,
    RelevanceCursorSupport<T> {

    companion object {
        const val SCORE_FIELD = "pdbscore"
    }

    override val idProperty: KProperty1<T, out Comparable<*>?>
        get() = idPropertyRef

    override val scoreAlias: String
        get() = scoreAliasValue

    override fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
    ): Predicate {
        return baseSpec.toPredicate(root, query, criteriaBuilder) ?: criteriaBuilder.conjunction()
    }

    override fun toCursorPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        cursor: TypedCompositeCursor<T>?,
    ): Predicate? {
        if (cursor == null) return null

        val cursorScore = cursor.values[scoreAlias]?.doubleValue ?: return null
        val scoreExpr = scoreExpression(root, criteriaBuilder)

        return when (direction) {
            Sort.Direction.DESC -> criteriaBuilder.lt(scoreExpr, cursorScore)
            Sort.Direction.ASC -> criteriaBuilder.gt(scoreExpr, cursorScore)
        }
    }

    override fun getSort(): Sort = Sort.unsorted()

    override fun extractCursor(entity: T): TypedCompositeCursor<T>? = null

    override fun buildProjection(
        root: Root<T>,
        query: CriteriaQuery<Tuple>,
        criteriaBuilder: CriteriaBuilder,
    ): CursorProjection<T, TypedCompositeCursor<T>> {
        @Suppress("UNCHECKED_CAST")
        val scoreSelection =
            scoreExpression(root, criteriaBuilder).alias(scoreAlias) as Selection<*>

        return CursorProjection(
            additionalSelections = listOf(scoreSelection),
            entityExtractor = { tuple -> tuple.get(0, domainClass) },
            cursorExtractor = { tuple ->
                val entity = tuple.get(0, domainClass)
                val idValue = idPropertyRef.get(entity)
                val scoreValue =
                    tuple.getOrNull(scoreAlias, Number::class.java)
                        ?: tuple.getOrNull(1, Number::class.java)
                val scoreAsString = scoreValue?.toString()
                TypedCompositeCursor.of(scoreAlias to scoreAsString, idPropertyRef.name to idValue)
            },
        )
    }

    override fun toJpaOrders(root: Root<T>, criteriaBuilder: CriteriaBuilder): List<Order> {
        val scoreExpr = scoreExpression(root, criteriaBuilder)
        val idPath = root.get<ID>(idPropertyRef.name)

        val scoreOrder =
            if (direction.isAscending) criteriaBuilder.asc(scoreExpr)
            else criteriaBuilder.desc(scoreExpr)
        val tieBreakerOrder = criteriaBuilder.asc(idPath)

        return listOf(scoreOrder, tieBreakerOrder)
    }

    override fun scoreExpression(root: Root<T>, cb: CriteriaBuilder): Expression<Double> =
        cb.function("pdb_score", Double::class.java, root.get<Any>(idPropertyRef.name))
}

fun <X> Tuple.getOrNull(alias: String, type: Class<X>): X? =
    runCatching { get(alias, type) }.getOrNull()

fun <X> Tuple.getOrNull(index: Int, type: Class<X>): X? =
    runCatching { get(index, type) }.getOrNull()
