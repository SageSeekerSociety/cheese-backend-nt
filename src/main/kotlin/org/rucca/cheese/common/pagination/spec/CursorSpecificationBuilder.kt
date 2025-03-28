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
 * Builder for creating cursor-based pagination specifications with explicit cursor types.
 *
 * @param T The entity type
 * @param C The concrete cursor type
 */
class CursorSpecificationBuilder<T, C : Cursor<T>>
private constructor(private val cursorType: Class<C>, vararg cursorBy: KProperty1<T, *>) {
    private val sortProperties = mutableListOf<Pair<KProperty1<T, *>, Sort.Direction>>()
    private var cursorProperties = mutableListOf<KProperty1<T, *>>()
    private var specification: Specification<T>? = null

    init {
        cursorProperties.addAll(cursorBy)
    }

    /** Add a sort property with direction. */
    fun sortBy(
        property: KProperty1<T, *>,
        direction: Sort.Direction = Sort.Direction.ASC,
    ): CursorSpecificationBuilder<T, C> {
        sortProperties.add(property to direction)
        return this
    }

    /** Set multiple sort properties at once. */
    fun sortBy(
        vararg properties: Pair<KProperty1<T, *>, Sort.Direction>
    ): CursorSpecificationBuilder<T, C> {
        sortProperties.clear()
        sortProperties.addAll(properties)
        return this
    }

    /** Set properties to use for cursor extraction. */
    fun cursorBy(vararg properties: KProperty1<T, *>): CursorSpecificationBuilder<T, C> {
        cursorProperties.clear()
        cursorProperties.addAll(properties)
        return this
    }

    /** Set filter specification. */
    fun specification(spec: Specification<T>): CursorSpecificationBuilder<T, C> {
        this.specification = spec
        return this
    }

    /** Set filter specification using lambda. */
    fun specification(
        specFn: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate?
    ): CursorSpecificationBuilder<T, C> {
        this.specification = Specification { root, query, cb -> specFn(root, query!!, cb) }
        return this
    }

    /** Build cursor specification with the specified cursor type. */
    @Suppress("UNCHECKED_CAST")
    fun build(): CursorSpecification<T, C> {
        require(sortProperties.isNotEmpty()) { "At least one sort property must be set" }

        // If no cursor properties specified, use sort properties
        if (cursorProperties.isEmpty()) {
            cursorProperties.addAll(sortProperties.map { it.first })
        }

        // Create base specification
        val baseSpec = specification ?: Specification { _, _, _ -> null }

        // Choose appropriate implementation based on cursor type and properties
        return when {
            SimpleCursor::class.java.isAssignableFrom(cursorType) && cursorProperties.size == 1 -> {
                createSimpleCursorSpecification(
                    cursorProperty = cursorProperties.first(),
                    sortProperty = sortProperties.first().first,
                    direction = sortProperties.first().second,
                    baseSpec = baseSpec,
                )
                    as CursorSpecification<T, C>
            }

            TypedCompositeCursor::class.java.isAssignableFrom(cursorType) -> {
                createCompositeCursorSpecification(
                    sortProperties = sortProperties,
                    cursorProperties = cursorProperties,
                    baseSpec = baseSpec,
                )
                    as CursorSpecification<T, C>
            }

            else -> {
                throw IllegalArgumentException(
                    "Cannot create cursor specification for cursor type: ${cursorType.simpleName} " +
                        "with ${cursorProperties.size} properties"
                )
            }
        }
    }

    /** Create simple cursor specification for single property. */
    private fun createSimpleCursorSpecification(
        cursorProperty: KProperty1<T, *>,
        sortProperty: KProperty1<T, *>,
        direction: Sort.Direction,
        baseSpec: Specification<T>,
    ): CursorSpecification<T, SimpleCursor<T, *>> {
        // Implementation remains the same, just typed specifically to SimpleCursor
        return object : CursorSpecification<T, SimpleCursor<T, *>> {
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
                cursor: SimpleCursor<T, *>?,
            ): Predicate? {
                if (cursor == null) return null

                // Create predicate
                val path = root.get<Comparable<Any>>(cursorProperty.name)
                val cursorValue = cursor.value as Comparable<Any>

                return when (direction) {
                    Sort.Direction.ASC -> criteriaBuilder.greaterThanOrEqualTo(path, cursorValue)
                    Sort.Direction.DESC -> criteriaBuilder.lessThanOrEqualTo(path, cursorValue)
                }
            }

            override fun getSort(): Sort {
                return Sort.by(direction, sortProperty.name)
            }

            @Suppress("UNCHECKED_CAST")
            override fun extractCursor(entity: T): SimpleCursor<T, *>? {
                val value = cursorProperty.get(entity) ?: return null
                return SimpleCursor<T, Any>(value)
            }
        }
    }

    /** Create composite cursor specification for multiple properties. */
    private fun createCompositeCursorSpecification(
        sortProperties: List<Pair<KProperty1<T, *>, Sort.Direction>>,
        cursorProperties: List<KProperty1<T, *>>,
        baseSpec: Specification<T>,
    ): CursorSpecification<T, TypedCompositeCursor<T>> {
        // Implementation remains similar, just typed specifically to TypedCompositeCursor
        return object : CursorSpecification<T, TypedCompositeCursor<T>> {
            // Implementation details remain the same but with specific TypedCompositeCursor type
            // ... implementation details ...

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
                // Existing implementation...
                if (cursor == null) return null

                // Extract values from cursor
                val cursorValues =
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

    companion object {
        /** Create a builder for simple cursor specifications */
        fun <T, V : Comparable<V>> simple(
            cursorProperty: KProperty1<T, V?>
        ): CursorSpecificationBuilder<T, SimpleCursor<T, V>> {
            @Suppress("UNCHECKED_CAST")
            return CursorSpecificationBuilder<T, SimpleCursor<T, V>>(
                SimpleCursor::class.java as Class<SimpleCursor<T, V>>,
                cursorProperty,
            )
        }

        /** Create a builder for composite cursor specifications */
        fun <T> composite(
            vararg cursorBy: KProperty1<T, *>
        ): CursorSpecificationBuilder<T, TypedCompositeCursor<T>> {
            @Suppress("UNCHECKED_CAST")
            return CursorSpecificationBuilder(
                TypedCompositeCursor::class.java as Class<TypedCompositeCursor<T>>,
                *cursorBy,
            )
        }
    }
}

/** Extension function to create simple cursor specification builder. */
fun <T, ID : Serializable, V : Comparable<V>> JpaRepository<T, ID>.simpleCursorSpec(
    property: KProperty1<T, V?>
): CursorSpecificationBuilder<T, SimpleCursor<T, V>> {
    return CursorSpecificationBuilder.simple(property).sortBy(property)
}

/** Extension function to create composite cursor specification builder. */
fun <T, ID : Serializable> JpaRepository<T, ID>.compositeCursorSpec(
    vararg properties: KProperty1<T, *>
): CursorSpecificationBuilder<T, TypedCompositeCursor<T>> {
    return CursorSpecificationBuilder.composite(*properties)
}
