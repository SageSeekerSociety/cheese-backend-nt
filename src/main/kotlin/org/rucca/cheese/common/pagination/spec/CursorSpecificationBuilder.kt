package org.rucca.cheese.common.pagination.spec

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.io.Serializable
import kotlin.reflect.KProperty1
import org.rucca.cheese.common.pagination.model.Cursor
import org.rucca.cheese.common.pagination.model.CursorValue
import org.rucca.cheese.common.pagination.model.SimpleCursor
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Builder for creating cursor-based pagination specifications.
 *
 * This builder supports both simple and composite cursors with a fluent API.
 *
 * @param T The entity type
 */
class CursorSpecificationBuilder<T> {
    private val sortProperties = mutableListOf<Pair<KProperty1<T, *>, Sort.Direction>>()
    private var cursorProperties = mutableListOf<KProperty1<T, *>>()
    private var specification: Specification<T>? = null

    /**
     * Add a sort property with direction.
     *
     * @param property The property to sort by
     * @param direction The sort direction
     * @return This builder for chaining
     */
    fun sortBy(
        property: KProperty1<T, *>,
        direction: Sort.Direction = Sort.Direction.ASC,
    ): CursorSpecificationBuilder<T> {
        sortProperties.add(property to direction)
        return this
    }

    /**
     * Set multiple sort properties at once.
     *
     * @param properties Property-direction pairs for sorting
     * @return This builder for chaining
     */
    fun sortBy(
        vararg properties: Pair<KProperty1<T, *>, Sort.Direction>
    ): CursorSpecificationBuilder<T> {
        sortProperties.clear()
        sortProperties.addAll(properties)
        return this
    }

    /**
     * Set properties to use for cursor extraction.
     *
     * @param properties Properties to extract for cursor values
     * @return This builder for chaining
     */
    fun cursorBy(vararg properties: KProperty1<T, *>): CursorSpecificationBuilder<T> {
        cursorProperties.clear()
        cursorProperties.addAll(properties)
        return this
    }

    /**
     * Set filter specification.
     *
     * @param spec JPA specification for filtering
     * @return This builder for chaining
     */
    fun specification(spec: Specification<T>): CursorSpecificationBuilder<T> {
        this.specification = spec
        return this
    }

    /**
     * Set filter specification using lambda.
     *
     * @param specFn Function that creates a predicate
     * @return This builder for chaining
     */
    fun specification(
        specFn: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate?
    ): CursorSpecificationBuilder<T> {
        this.specification = Specification { root, query, cb -> specFn(root, query!!, cb) }
        return this
    }

    /**
     * Build cursor specification, automatically choosing simple or composite implementation.
     *
     * @return Cursor specification
     */
    fun build(): CursorSpecification<T, Cursor<T>> {
        require(sortProperties.isNotEmpty()) { "At least one sort property must be set" }

        // If no cursor properties specified, use sort properties
        if (cursorProperties.isEmpty()) {
            cursorProperties.addAll(sortProperties.map { it.first })
        }

        // Create base specification
        val baseSpec = specification ?: Specification { _, _, _ -> null }

        // Choose appropriate implementation based on properties count
        return if (cursorProperties.size == 1 && sortProperties.size == 1) {
            createSimpleCursorSpecification(
                cursorProperty = cursorProperties.first(),
                sortProperty = sortProperties.first().first,
                direction = sortProperties.first().second,
                baseSpec = baseSpec,
            )
        } else {
            createCompositeCursorSpecification(
                sortProperties = sortProperties,
                cursorProperties = cursorProperties,
                baseSpec = baseSpec,
            )
        }
    }

    /** Create simple cursor specification for single property. */
    private fun createSimpleCursorSpecification(
        cursorProperty: KProperty1<T, *>,
        sortProperty: KProperty1<T, *>,
        direction: Sort.Direction,
        baseSpec: Specification<T>,
    ): CursorSpecification<T, Cursor<T>> {
        return object : CursorSpecification<T, Cursor<T>> {
            override fun toPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                criteriaBuilder: CriteriaBuilder,
            ): Predicate {
                return baseSpec.toPredicate(root, query, criteriaBuilder)
                    ?: criteriaBuilder.conjunction()
            }

            @Suppress("UNCHECKED_CAST")
            override fun toCursorPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                criteriaBuilder: CriteriaBuilder,
                cursor: Cursor<T>?,
            ): Predicate? {
                if (cursor == null) return null

                // Extract value from cursor
                val cursorValue =
                    when (cursor) {
                        is SimpleCursor<*, *> -> cursor.value
                        is TypedCompositeCursor<*> ->
                            cursor.values[cursorProperty.name]?.let {
                                when (it) {
                                    is CursorValue.StringValue -> it.value
                                    is CursorValue.LongValue -> it.value
                                    is CursorValue.DoubleValue -> it.value
                                    is CursorValue.BooleanValue -> it.value
                                    is CursorValue.TimestampValue -> it.value
                                    CursorValue.NullValue -> null
                                }
                            }
                    } ?: return null

                // Create predicate
                val path = root.get<Comparable<Any>>(cursorProperty.name)

                return when (direction) {
                    Sort.Direction.ASC ->
                        criteriaBuilder.greaterThanOrEqualTo(path, cursorValue as Comparable<Any>)
                    Sort.Direction.DESC ->
                        criteriaBuilder.lessThanOrEqualTo(path, cursorValue as Comparable<Any>)
                }
            }

            override fun getSort(): Sort {
                return Sort.by(direction, sortProperty.name)
            }

            override fun extractCursor(entity: T): Cursor<T>? {
                val value = cursorProperty.get(entity) ?: return null

                return when (value) {
                    is String,
                    is Number,
                    is Boolean -> SimpleCursor<T, Any>(value)
                    else ->
                        TypedCompositeCursor<T>(mapOf(cursorProperty.name to CursorValue.of(value)))
                }
            }
        }
    }

    /** Create composite cursor specification for multiple properties. */
    private fun createCompositeCursorSpecification(
        sortProperties: List<Pair<KProperty1<T, *>, Sort.Direction>>,
        cursorProperties: List<KProperty1<T, *>>,
        baseSpec: Specification<T>,
    ): CursorSpecification<T, Cursor<T>> {
        return object : CursorSpecification<T, Cursor<T>> {
            override fun toPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                criteriaBuilder: CriteriaBuilder,
            ): Predicate {
                return baseSpec.toPredicate(root, query, criteriaBuilder)
                    ?: criteriaBuilder.conjunction()
            }

            @Suppress("UNCHECKED_CAST")
            override fun toCursorPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                criteriaBuilder: CriteriaBuilder,
                cursor: Cursor<T>?,
            ): Predicate? {
                if (cursor == null) return null

                // Extract values from cursor
                val cursorValues =
                    when (cursor) {
                        is SimpleCursor<*, *> -> {
                            // Handle single value cursor - assume it's for first property
                            mapOf(cursorProperties.first().name to cursor.value)
                        }
                        is TypedCompositeCursor<*> -> {
                            // Convert CursorValue to raw values
                            cursor.values.mapValues { (_, value) ->
                                when (value) {
                                    is CursorValue.StringValue -> value.value
                                    is CursorValue.LongValue -> value.value
                                    is CursorValue.DoubleValue -> value.value
                                    is CursorValue.BooleanValue -> value.value
                                    is CursorValue.TimestampValue -> value.value
                                    CursorValue.NullValue -> null
                                }
                            }
                        }
                        else -> return null
                    }

                if (cursorValues.isEmpty()) return null

                // Build OR conditions for each progressive property combination
                val predicates = mutableListOf<Predicate>()

                // Build predicates based on sort properties
                for (i in sortProperties.indices) {
                    val (prop, direction) = sortProperties[i]
                    val propName = prop.name
                    val cursorValue = cursorValues[propName] ?: continue

                    // Equal conditions for preceding properties
                    val equalPredicates =
                        (0 until i).mapNotNull { j ->
                            val (prevProp, _) = sortProperties[j]
                            val prevPropName = prevProp.name
                            val prevValue = cursorValues[prevPropName] ?: return@mapNotNull null

                            criteriaBuilder.equal(root.get<Any>(prevPropName), prevValue)
                        }

                    // Comparison condition for current property
                    val path = root.get<Comparable<Any>>(propName)
                    val compPredicate =
                        when (direction) {
                            Sort.Direction.ASC ->
                                criteriaBuilder.greaterThan(path, cursorValue as Comparable<Any>)
                            Sort.Direction.DESC ->
                                criteriaBuilder.lessThan(path, cursorValue as Comparable<Any>)
                        }

                    // Combine conditions
                    val combined =
                        if (equalPredicates.isEmpty()) {
                            compPredicate
                        } else {
                            criteriaBuilder.and(
                                criteriaBuilder.and(*equalPredicates.toTypedArray()),
                                compPredicate,
                            )
                        }

                    predicates.add(combined)
                }

                // Add equality case for the cursor record itself
                val allEqualPredicates =
                    sortProperties.mapNotNull { (prop, _) ->
                        val propName = prop.name
                        val value = cursorValues[propName] ?: return@mapNotNull null

                        criteriaBuilder.equal(root.get<Any>(propName), value)
                    }

                if (allEqualPredicates.isNotEmpty()) {
                    predicates.add(criteriaBuilder.and(*allEqualPredicates.toTypedArray()))
                }

                return if (predicates.isEmpty()) null
                else criteriaBuilder.or(*predicates.toTypedArray())
            }

            override fun getSort(): Sort {
                return Sort.by(sortProperties.map { (prop, dir) -> Sort.Order(dir, prop.name) })
            }

            override fun extractCursor(entity: T): TypedCompositeCursor<T>? {
                // Extract all cursor properties from entity
                val values =
                    cursorProperties.associate { prop ->
                        prop.name to CursorValue.of(prop.get(entity))
                    }

                return TypedCompositeCursor(values)
            }
        }
    }
}

/**
 * Extension function to create cursor specification builder.
 *
 * @return New builder instance
 */
fun <T, ID : Serializable> JpaRepository<T, ID>.cursorSpec(): CursorSpecificationBuilder<T> {
    return CursorSpecificationBuilder<T>()
}

/**
 * Extension function for backward compatibility with single property cursors.
 *
 * @param property The cursor property
 * @return Configured builder instance
 */
@Deprecated(
    "Use cursorSpec() instead for more flexibility",
    ReplaceWith("cursorSpec().sortBy(property).cursorBy(property)"),
)
fun <T, ID : Serializable, V : Comparable<V>> JpaRepository<T, ID>.cursorSpec(
    property: KProperty1<T, V?>
): CursorSpecificationBuilder<T> {
    return CursorSpecificationBuilder<T>().sortBy(property).cursorBy(property)
}
