package org.rucca.cheese.common.query.internal.pagination

import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.query.model.PropertySort
import org.rucca.cheese.common.query.model.RelevanceSort
import org.rucca.cheese.common.query.model.SortDescriptor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

class RelevanceCursorStrategy<T : Any> : CursorStrategy<T, TypedCompositeCursor<T>> {
    override fun build(
        entityClass: Class<T>,
        idProperty: KProperty1<T, Comparable<*>?>,
        sorts: List<SortDescriptor<T>>,
        baseSpecification: Specification<T>,
    ): CursorSpecification<T, TypedCompositeCursor<T>> {
        val relevanceSorts = sorts.filterIsInstance<RelevanceSort<T>>()
        require(relevanceSorts.size == 1) {
            "Relevance-based cursor strategy requires exactly one relevance sort descriptor"
        }

        val descriptor = relevanceSorts.first()
        val propertySorts = sorts.filterIsInstance<PropertySort<T>>()
        require(
            propertySorts.all { sort ->
                sort.property == idProperty && sort.direction == Sort.Direction.ASC
            }
        ) {
            "Relevance-based cursor strategy only supports ID ascending property tie-breakers"
        }

        @Suppress("UNCHECKED_CAST")
        val comparableIdProperty =
            idProperty as? KProperty1<T, out Comparable<*>?>
                ?: error("Relevance-based cursor strategy requires comparable id property")

        return ParadeCursorCursorSpecification(
            domainClass = entityClass,
            idPropertyRef = comparableIdProperty,
            baseSpec = baseSpecification,
            direction = descriptor.direction,
            scoreAliasValue = descriptor.scoreAlias ?: DEFAULT_SCORE_ALIAS,
        )
    }

    private class ParadeCursorCursorSpecification<T : Any>(
        private val domainClass: Class<T>,
        private val idPropertyRef: KProperty1<T, out Comparable<*>?>,
        private val baseSpec: Specification<T>,
        private val direction: Sort.Direction,
        private val scoreAliasValue: String,
    ) :
        CursorSpecification<T, TypedCompositeCursor<T>>,
        CursorProjectionSupport<T, TypedCompositeCursor<T>>,
        RelevanceCursorSupport<T> {

        override val idProperty: KProperty1<T, out Comparable<*>?>
            get() = idPropertyRef

        override val scoreAlias: String
            get() = scoreAliasValue

        override fun toPredicate(
            root: Root<T>,
            query: CriteriaQuery<*>,
            criteriaBuilder: CriteriaBuilder,
        ): Predicate {
            return baseSpec.toPredicate(root, query, criteriaBuilder)
                ?: criteriaBuilder.conjunction()
        }

        override fun toCursorPredicate(
            root: Root<T>,
            query: CriteriaQuery<*>,
            criteriaBuilder: CriteriaBuilder,
            cursor: TypedCompositeCursor<T>?,
        ): Predicate? {
            if (cursor == null) return null

            val scoreExpr = scoreExpression(root, criteriaBuilder)
            val cursorScore = cursor.values[scoreAlias]?.doubleValue ?: return null

            return when (direction) {
                Sort.Direction.ASC -> criteriaBuilder.greaterThan(scoreExpr, cursorScore)
                Sort.Direction.DESC -> criteriaBuilder.lessThan(scoreExpr, cursorScore)
            }
        }

        override fun getSort(): Sort = Sort.unsorted()

        override fun extractCursor(entity: T): TypedCompositeCursor<T>? = null

        override fun buildProjection(
            root: Root<T>,
            query: CriteriaQuery<Tuple>,
            criteriaBuilder: CriteriaBuilder,
        ): CursorProjection<T, TypedCompositeCursor<T>> {
            val scoreSelection = scoreExpression(root, criteriaBuilder).alias(scoreAlias)
            return CursorProjection(
                additionalSelections = listOf(scoreSelection),
                entityExtractor = { tuple -> tuple.get(0, domainClass) },
                cursorExtractor = { tuple ->
                    val entity = tuple.get(0, domainClass)
                    val idValue = idPropertyRef.get(entity)
                    val scoreValue = tuple.get(scoreAlias, Number::class.java)?.toDouble()
                    TypedCompositeCursor.of(scoreAlias to scoreValue, idPropertyRef.name to idValue)
                },
            )
        }

        override fun toJpaOrders(
            root: Root<T>,
            criteriaBuilder: CriteriaBuilder,
        ): List<jakarta.persistence.criteria.Order> {
            val scoreExpr = scoreExpression(root, criteriaBuilder)
            @Suppress("UNCHECKED_CAST") val idPath = root.get<Comparable<Any?>>(idPropertyRef.name)
            val scoreOrder =
                if (direction.isAscending) criteriaBuilder.asc(scoreExpr)
                else criteriaBuilder.desc(scoreExpr)
            val tieBreaker = criteriaBuilder.asc(idPath)
            return listOf(scoreOrder, tieBreaker)
        }

        override fun scoreExpression(
            root: Root<T>,
            criteriaBuilder: CriteriaBuilder,
        ): Expression<Double> =
            criteriaBuilder.function(
                "pdb_score",
                Double::class.java,
                root.get<Any>(idPropertyRef.name),
            )
    }

    companion object {
        private const val DEFAULT_SCORE_ALIAS = "pdbscore"
    }
}
