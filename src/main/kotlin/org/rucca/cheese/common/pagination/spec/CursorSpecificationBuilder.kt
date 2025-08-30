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
import org.rucca.cheese.common.pagination.util.JpaUtils
import org.rucca.cheese.common.pagination.util.ReflectionUtils
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
private constructor(private val cursorType: Class<C>, vararg cursorBy: String) {
    private val sortProperties = mutableListOf<Pair<String, Sort.Direction>>()
    private var cursorProperties = mutableListOf<String>()
    private var specification: Specification<T>? = null

    init {
        cursorProperties.addAll(cursorBy)
    }

    /**
     * Add a sort property using KProperty1 (compile-time safe).
     *
     * @param property The property reference.
     * @param direction Sort direction (default: ASC).
     */
    fun sortBy(
        property: KProperty1<T, *>,
        direction: Sort.Direction = Sort.Direction.ASC,
    ): CursorSpecificationBuilder<T, C> {
        sortProperties.add(property.name to direction) // Store by name
        // If cursor properties haven't been explicitly set by path, implicitly add sort property
        // name
        if (cursorProperties.isEmpty() || cursorProperties.all { !it.contains('.') }) {
            if (!cursorProperties.contains(property.name)) {
                // Heuristic: If only simple properties were added before, add this one too.
                // Avoid adding if path-based properties were already set explicitly.
                cursorProperties.add(property.name)
            }
        }
        return this
    }

    /**
     * Set multiple sort properties using KProperty1 (compile-time safe). Clears existing sort
     * properties.
     */
    fun sortBy(
        vararg properties: Pair<KProperty1<T, *>, Sort.Direction>
    ): CursorSpecificationBuilder<T, C> {
        sortProperties.clear()
        properties.forEach { (prop, dir) -> sortProperties.add(prop.name to dir) }
        // Implicitly update cursor properties if needed (similar logic to single sortBy)
        if (cursorProperties.isEmpty() || cursorProperties.all { !it.contains('.') }) {
            cursorProperties.clear()
            cursorProperties.addAll(sortProperties.map { it.first })
        }
        return this
    }

    /**
     * Set properties to use for cursor extraction using KProperty1 (compile-time safe). Clears
     * existing cursor properties. Note: Best for simple properties of the root entity T. For
     * related entity properties, use `cursorByPath`.
     */
    fun cursorBy(vararg properties: KProperty1<T, *>): CursorSpecificationBuilder<T, C> {
        cursorProperties.clear()
        cursorProperties.addAll(properties.map { it.name })
        return this
    }

    /**
     * Add a sort property using a String path (flexible, allows "related.property"). Use this for
     * sorting by properties of related entities. Note: Loses compile-time safety compared to
     * KProperty1 version.
     *
     * @param path The property path (e.g., "name", "discussion.createdAt").
     * @param direction Sort direction (default: ASC).
     */
    fun sortByPath(
        path: String,
        direction: Sort.Direction = Sort.Direction.ASC,
    ): CursorSpecificationBuilder<T, C> {
        sortProperties.add(path to direction)
        // If cursor properties haven't been explicitly set, implicitly add sort path
        if (cursorProperties.isEmpty()) {
            cursorProperties.add(path)
        }
        return this
    }

    /**
     * Set multiple sort properties using String paths (flexible). Clears existing sort properties.
     */
    fun sortByPath(
        vararg properties: Pair<String, Sort.Direction>
    ): CursorSpecificationBuilder<T, C> {
        sortProperties.clear()
        sortProperties.addAll(properties)
        // Implicitly update cursor properties if needed
        if (cursorProperties.isEmpty()) {
            cursorProperties.clear()
            cursorProperties.addAll(sortProperties.map { it.first })
        }
        return this
    }

    /**
     * Set properties (using String paths) to use for cursor extraction (flexible, allows
     * "related.property"). Clears existing cursor properties. This is the recommended method when
     * dealing with related entity properties.
     *
     * @param paths The property paths (e.g., "name", "discussion.id").
     */
    fun cursorByPath(vararg paths: String): CursorSpecificationBuilder<T, C> {
        cursorProperties.clear()
        cursorProperties.addAll(paths)
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

        // If no cursor properties specified explicitly, default to using sort properties' paths
        if (cursorProperties.isEmpty()) {
            cursorProperties.addAll(sortProperties.map { it.first })
        }

        // Ensure cursor properties are consistent (e.g., all paths exist conceptually) - basic
        // check
        require(cursorProperties.isNotEmpty()) {
            "Cursor properties cannot be empty after defaulting."
        }

        // Create base specification
        val baseSpec = specification ?: Specification { _, _, _ -> null }

        // Use String paths for internal implementation details
        val finalSortProperties = sortProperties.toList() // Immutable copy
        val finalCursorProperties = cursorProperties.toList() // Immutable copy

        // Choose appropriate implementation based on cursor type and properties
        // Note: SimpleCursor is generally suitable only for single, non-nested properties.
        // CompositeCursor is more general purpose, especially with paths.
        return when {
            // SimpleCursor: Only if configured with exactly one non-nested property path.
            SimpleCursor::class.java.isAssignableFrom(cursorType) &&
                finalCursorProperties.size == 1 &&
                !finalCursorProperties.first().contains('.') -> {
                createSimpleCursorSpecification(
                    // Use the single path for both cursor and sort (common case for SimpleCursor)
                    cursorPath = finalCursorProperties.first(),
                    sortPath =
                        finalSortProperties.first().first, // Assumes first sort matches cursor
                    direction = finalSortProperties.first().second,
                    baseSpec = baseSpec,
                )
                    as CursorSpecification<T, C>
            }

            // TypedCompositeCursor: Handles single or multiple properties, including nested paths.
            TypedCompositeCursor::class.java.isAssignableFrom(cursorType) -> {
                createCompositeCursorSpecification(
                    sortProperties = finalSortProperties,
                    cursorPaths = finalCursorProperties,
                    baseSpec = baseSpec,
                )
                    as CursorSpecification<T, C>
            }

            else -> {
                throw IllegalArgumentException(
                    "Cannot create cursor specification for cursor type: ${cursorType.simpleName} " +
                        "with cursor paths: $finalCursorProperties. Ensure compatibility (e.g., SimpleCursor needs one non-nested path)."
                )
            }
        }
    }

    /** Create simple cursor specification for a single, non-nested property path. */
    private fun createSimpleCursorSpecification(
        cursorPath: String, // Now uses path
        sortPath: String, // Now uses path
        direction: Sort.Direction,
        baseSpec: Specification<T>,
    ): CursorSpecification<T, SimpleCursor<T, *>> {
        return object : CursorSpecification<T, SimpleCursor<T, *>> {
            override fun toPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                cb: CriteriaBuilder,
            ): Predicate {
                return baseSpec.toPredicate(root, query, cb) ?: cb.conjunction()
            }

            @Suppress("UNCHECKED_CAST")
            override fun toCursorPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                cb: CriteriaBuilder,
                cursor: SimpleCursor<T, *>?,
            ): Predicate? {
                if (cursor == null) return null
                val cursorValue = cursor.value as? Comparable<Any> ?: return null // Safe cast

                // Use helper to get the actual Path object from the path string
                val path = JpaUtils.getPath<Comparable<Any>>(root, cursorPath) // Use helper

                return when (direction) {
                    Sort.Direction.ASC -> cb.greaterThanOrEqualTo(path, cursorValue)
                    Sort.Direction.DESC -> cb.lessThanOrEqualTo(path, cursorValue)
                }
            }

            override fun getSort(): Sort {
                return Sort.by(direction, sortPath) // Sort uses path string directly
            }

            override fun extractCursor(entity: T): SimpleCursor<T, *>? {
                // Use reflection helper to get value based on path string
                val value = ReflectionUtils.getPropertyValue(entity, cursorPath) ?: return null
                return SimpleCursor<T, Any>(value)
            }
        }
    }

    /** Create composite cursor specification for multiple property paths (including nested). */
    private fun createCompositeCursorSpecification(
        sortProperties: List<Pair<String, Sort.Direction>>, // Uses paths
        cursorPaths: List<String>, // Uses paths
        baseSpec: Specification<T>,
    ): CursorSpecification<T, TypedCompositeCursor<T>> {
        return object : CursorSpecification<T, TypedCompositeCursor<T>> {
            override fun toPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                cb: CriteriaBuilder,
            ): Predicate {
                return baseSpec.toPredicate(root, query, cb) ?: cb.conjunction()
            }

            override fun toCursorPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                cb: CriteriaBuilder,
                cursor: TypedCompositeCursor<T>?,
            ): Predicate? {
                if (cursor == null) return null

                // Extract raw values from cursor map, keyed by path strings
                val cursorValues =
                    cursor.values.mapValues { (_, valueWrapper) ->
                        valueWrapper.unwrap() // Assumes CursorValue has an unwrap() method
                    }

                if (cursorValues.isEmpty()) return null

                val predicates = mutableListOf<Predicate>()

                // Build OR conditions based on sort properties (paths)
                for (i in sortProperties.indices) {
                    val (propPath, direction) = sortProperties[i]
                    val cursorValue =
                        cursorValues[propPath]
                            ?: continue // Skip if cursor doesn't have value for this sort path

                    // Equal conditions for preceding sort properties
                    val equalPredicates =
                        (0 until i).mapNotNull { j ->
                            val (prevPropPath, _) = sortProperties[j]
                            val prevValue = cursorValues[prevPropPath] // Get value using path
                            if (prevValue == null) {
                                // If previous cursor value is null, equality check is IS NULL
                                cb.isNull(JpaUtils.getPath<Any>(root, prevPropPath))
                            } else {
                                cb.equal(JpaUtils.getPath<Any>(root, prevPropPath), prevValue)
                            }
                            // Note: If a previous required cursor value is missing entirely from
                            // cursorValues map,
                            // this specific OR branch might be invalid. Consider adding checks or
                            // ensure cursor is complete.
                        }

                    // Comparison condition for the current sort property
                    val currentPath =
                        JpaUtils.getPath<Comparable<Any>>(root, propPath) // Use helper
                    val compPredicate =
                        try {
                            when (direction) {
                                Sort.Direction.ASC ->
                                    cb.greaterThan(currentPath, cursorValue as Comparable<Any>)
                                Sort.Direction.DESC ->
                                    cb.lessThan(currentPath, cursorValue as Comparable<Any>)
                            }
                        } catch (e: ClassCastException) {
                            // Handle case where cursor value is not comparable (should not happen
                            // if used correctly)
                            throw IllegalArgumentException(
                                "Property '$propPath' value in cursor is not comparable.",
                                e,
                            )
                        }

                    // Combine equal predicates and comparison predicate
                    val combined =
                        if (equalPredicates.isEmpty()) {
                            compPredicate
                        } else {
                            cb.and(*equalPredicates.toTypedArray(), compPredicate)
                        }
                    predicates.add(combined)
                }

                // Add equality case for the cursor record itself (all sort properties must be
                // equal)
                // This is crucial for >= / <= logic in cursor pagination
                val allEqualPredicates =
                    sortProperties.mapNotNull { (propPath, _) ->
                        val value = cursorValues[propPath]
                        if (value == null) {
                            cb.isNull(JpaUtils.getPath<Any>(root, propPath))
                        } else {
                            cb.equal(JpaUtils.getPath<Any>(root, propPath), value)
                        }
                    }

                if (
                    allEqualPredicates.isNotEmpty() &&
                        allEqualPredicates.size == sortProperties.size
                ) {
                    // Only add if all sort properties could be evaluated for equality
                    predicates.add(cb.and(*allEqualPredicates.toTypedArray()))
                }

                return if (predicates.isEmpty()) null else cb.or(*predicates.toTypedArray())
            }

            override fun getSort(): Sort {
                // Sort uses path strings directly
                return Sort.by(sortProperties.map { (path, dir) -> Sort.Order(dir, path) })
            }

            override fun extractCursor(entity: T): TypedCompositeCursor<T>? {
                // Extract values based on cursorPaths using reflection helper
                val values =
                    cursorPaths.associateWith { path ->
                        // Use reflection helper to get potentially nested value
                        val value = ReflectionUtils.getPropertyValue(entity, path)
                        CursorValue.of(value) // Wrap value (handle nulls inside CursorValue.of)
                    }

                // Check if any essential value extraction failed (optional, depends on
                // ReflectionUtils behavior)
                // if (values.any { it.value == null && !cursorPaths.contains(it.key) }) { //
                // Example check
                //     return null // Or log warning
                // }

                return TypedCompositeCursor(values)
            }
        }
    }

    companion object {
        /** Create a builder for simple cursor specifications using KProperty1. */
        fun <T, V : Comparable<V>> simple(
            cursorProperty: KProperty1<T, V?>
        ): CursorSpecificationBuilder<T, SimpleCursor<T, V>> {
            @Suppress("UNCHECKED_CAST")
            return CursorSpecificationBuilder<T, SimpleCursor<T, V>>(
                    SimpleCursor::class.java as Class<SimpleCursor<T, V>>,
                    cursorProperty.name, // Store as path internally
                )
                .sortBy(cursorProperty) // Default sort by the same property
        }

        /** Create a builder for composite cursor specifications using KProperty1. */
        fun <T> composite(
            vararg cursorBy: KProperty1<T, *>
        ): CursorSpecificationBuilder<T, TypedCompositeCursor<T>> {
            @Suppress("UNCHECKED_CAST")
            return CursorSpecificationBuilder(
                TypedCompositeCursor::class.java as Class<TypedCompositeCursor<T>>,
                *(cursorBy.map { it.name }.toTypedArray()), // Store as paths internally
            )
        }

        /** Create a builder for composite cursor specifications using String paths. */
        fun <T> compositeWithPath(
            vararg cursorByPaths: String
        ): CursorSpecificationBuilder<T, TypedCompositeCursor<T>> {
            require(cursorByPaths.isNotEmpty()) { "At least one cursor path must be provided." }
            @Suppress("UNCHECKED_CAST")
            return CursorSpecificationBuilder(
                TypedCompositeCursor::class.java as Class<TypedCompositeCursor<T>>,
                *cursorByPaths, // Store paths directly
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
