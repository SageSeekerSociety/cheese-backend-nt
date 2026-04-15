package org.rucca.cheese.common.query.internal.pagination

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf
import org.rucca.cheese.common.query.model.PropertySort
import org.rucca.cheese.common.query.model.SortDescriptor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

class PropertyBasedCursorStrategy<T : Any> : CursorStrategy<T, TypedCompositeCursor<T>> {
    override fun build(
        entityClass: Class<T>,
        idProperty: KProperty1<T, Comparable<*>?>,
        sorts: List<SortDescriptor<T>>,
        baseSpecification: Specification<T>,
    ): CursorSpecification<T, TypedCompositeCursor<T>> {
        val propertySorts = sorts.filterIsInstance<PropertySort<T>>()
        require(propertySorts.isNotEmpty()) {
            "Property-based cursor strategy requires at least one property sort descriptor"
        }

        val sortSpecs = buildSortSpecs(propertySorts, idProperty)
        return PropertyCursorSpecification(idProperty, sortSpecs, baseSpecification)
    }

    private fun buildSortSpecs(
        propertySorts: List<PropertySort<T>>,
        idProperty: KProperty1<T, Comparable<*>?>,
    ): List<PropertySortSpec<T>> {
        val aliasCounts = mutableMapOf<String, Int>()
        val specs = mutableListOf<PropertySortSpec<T>>()

        propertySorts.forEach { sort ->
            specs +=
                PropertySortSpec(
                    property = sort.property,
                    direction = sort.direction,
                    nullHandling = sort.nullHandling,
                    alias = nextAlias(sort.property.name, aliasCounts),
                )
        }

        specs +=
            PropertySortSpec(
                property = idProperty,
                direction = Sort.Direction.ASC,
                nullHandling = Sort.NullHandling.NATIVE,
                alias = nextAlias(idProperty.name, aliasCounts),
                isTieBreaker = true,
            )

        return specs
    }

    private fun nextAlias(base: String, counts: MutableMap<String, Int>): String {
        val current = counts.getOrDefault(base, -1) + 1
        counts[base] = current
        return if (current == 0) base else "$base#$current"
    }
}

private data class PropertySortSpec<T : Any>(
    val property: KProperty1<T, *>,
    val direction: Sort.Direction,
    val nullHandling: Sort.NullHandling?,
    val alias: String,
    val isTieBreaker: Boolean = false,
)

private class PropertyCursorSpecification<T : Any>(
    private val idProperty: KProperty1<T, Comparable<*>?>,
    private val sortSpecs: List<PropertySortSpec<T>>,
    private val baseSpecification: Specification<T>,
) : CursorSpecification<T, TypedCompositeCursor<T>> {

    private val jpaSort: Sort = buildJpaSort(sortSpecs)

    override fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
    ): Predicate {
        return baseSpecification.toPredicate(root, query, criteriaBuilder)
            ?: criteriaBuilder.conjunction()
    }

    override fun toCursorPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        cursor: TypedCompositeCursor<T>?,
    ): Predicate? {
        if (cursor == null) return null
        val values = cursor.values
        if (values.isEmpty()) return null

        val predicates = mutableListOf<Predicate>()
        val equalityChain = mutableListOf<Predicate>()

        sortSpecs.forEach { spec ->
            val rawValue = values[spec.alias]
            @Suppress("UNCHECKED_CAST")
            val path = root.get<Comparable<Any?>>(spec.property.name) as Path<Comparable<Any?>>
            if (rawValue == null || rawValue.unwrap() == null) {
                val nullsFirst =
                    when (spec.nullHandling) {
                        Sort.NullHandling.NULLS_FIRST -> true
                        Sort.NullHandling.NULLS_LAST -> false
                        // For Postgres defaults: NULLS_LAST on ASC, NULLS_FIRST on DESC
                        null,
                        Sort.NullHandling.NATIVE -> spec.direction == Sort.Direction.DESC
                    }
                // When nulls sort first, rows after the null boundary are any non-null values
                val advance: Predicate? = if (nullsFirst) criteriaBuilder.isNotNull(path) else null
                if (advance != null) {
                    predicates +=
                        if (equalityChain.isEmpty()) advance
                        else criteriaBuilder.and(*equalityChain.toTypedArray(), advance)
                }
                // Retain an equality on NULL so deeper specs can still advance
                equalityChain += criteriaBuilder.isNull(path)
                return@forEach
            }
            val value =
                convertCursorValueToPropertyType(rawValue, spec.property) as? Comparable<Any?>
                    ?: return@forEach
            val comparison =
                when (spec.direction) {
                    Sort.Direction.ASC -> criteriaBuilder.greaterThan(path, value)
                    Sort.Direction.DESC -> criteriaBuilder.lessThan(path, value)
                }
            if (equalityChain.isEmpty()) {
                predicates += comparison
            } else {
                predicates += criteriaBuilder.and(*equalityChain.toTypedArray(), comparison)
            }
            equalityChain += criteriaBuilder.equal(path, value)
        }

        return if (predicates.isEmpty()) null else criteriaBuilder.or(*predicates.toTypedArray())
    }

    override fun getSort(): Sort = jpaSort

    override fun extractCursor(entity: T): TypedCompositeCursor<T>? {
        val values = linkedMapOf<String, CursorValue>()
        sortSpecs.forEach { spec ->
            val value = spec.property.get(entity)
            values[spec.alias] = CursorValue.of(value)
        }
        return if (values.isEmpty()) null else TypedCompositeCursor(values)
    }

    private fun buildJpaSort(specs: List<PropertySortSpec<T>>): Sort {
        val orders =
            specs
                .filter { !it.isTieBreaker }
                .map { sortSpec ->
                    var order = Sort.Order(sortSpec.direction, sortSpec.property.name)
                    when (sortSpec.nullHandling) {
                        Sort.NullHandling.NULLS_FIRST -> order = order.nullsFirst()
                        Sort.NullHandling.NULLS_LAST -> order = order.nullsLast()
                        else -> {}
                    }
                    order
                }
                .toMutableList()

        if (orders.none { it.property.equals(idProperty.name, ignoreCase = false) }) {
            orders += Sort.Order(Sort.Direction.ASC, idProperty.name)
        }

        return Sort.by(orders)
    }

    /**
     * Converts a cursor value to the appropriate type for comparison with the property. Handles
     * type mismatches, particularly for timestamp values that need to be converted back to
     * LocalDateTime for comparison.
     */
    private fun convertCursorValueToPropertyType(
        cursorValue: CursorValue,
        property: KProperty1<T, *>,
    ): Any? {
        val unwrapped = cursorValue.unwrap()

        if (property.returnType.isSubtypeOf(typeOf<LocalDateTime?>())) {
            val epochMillis =
                when (unwrapped) {
                    is Long -> unwrapped
                    is Number -> unwrapped.toLong()
                    is String -> unwrapped.toLongOrNull()
                    else -> null
                }

            if (epochMillis != null) {
                return Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            }
        }

        return unwrapped
    }
}
