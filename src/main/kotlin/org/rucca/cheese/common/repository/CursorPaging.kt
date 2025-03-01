package org.rucca.cheese.common.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.io.Serializable
import kotlin.reflect.KProperty1
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.PageDTO
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * Container for pagination results with strongly typed cursor support.
 *
 * @param T the entity type
 * @param C the cursor type
 */
data class CursorPage<T, C : Any>(val content: List<T>, val pageInfo: CursorPageInfo<C>)

/**
 * Pagination metadata with strongly typed cursor.
 *
 * @param C the cursor type
 */
data class CursorPageInfo<C : Any>(
    val cursor: C? = null,
    val pageSize: Int,
    val hasPrevious: Boolean = false,
    val previousCursor: C? = null,
    val hasNext: Boolean = false,
    val nextCursor: C? = null,
)

/**
 * Interface for cursor-based pagination specifications.
 *
 * @param T the entity type
 * @param C the cursor type, must be comparable
 */
interface CursorSpecification<T, C : Comparable<C>> {
    /** Returns the property used for cursor-based sorting. */
    fun getCursorProperty(): KProperty1<T, C?>

    /** Returns the property used for sorting. */
    fun getSortProperty(): KProperty1<T, *>

    /** Returns the sort direction for the cursor property. */
    fun getDirection(): Sort.Direction

    /** Creates the base predicate for filtering entities. */
    fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
    ): Predicate

    /** Creates the cursor-based predicate for pagination. */
    fun toCursorPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        cursor: C?,
    ): Predicate?

    /** Returns the sort specification for the query. */
    fun getSort(): Sort

    /** Extracts the cursor value from an entity. */
    fun extractCursor(entity: T): C?
}

/**
 * Builder for creating cursor pagination specifications.
 *
 * @param T the entity type
 * @param C the cursor type
 */
class CursorSpecificationBuilder<T, C : Comparable<C>> {
    private var sortProperty: KProperty1<T, *>? = null
    private var cursorProperty: KProperty1<T, C?>? = null
    private var direction: Sort.Direction = Sort.Direction.ASC
    private var additionalSort: Sort = Sort.unsorted()
    private var specification: Specification<T>? = null
    private var cursorExtractor: ((T) -> C?)? = null
    private var customCursorPredicate:
        ((Root<T>, CriteriaQuery<*>, CriteriaBuilder, C?) -> Predicate?)? =
        null

    /** Sets the property to use for sorting. */
    fun sortBy(property: KProperty1<T, *>): CursorSpecificationBuilder<T, C> {
        this.sortProperty = property
        return this
    }

    /** Sets the property to use for sorting. */
    fun sortBy(
        property: KProperty1<T, *>,
        direction: Sort.Direction,
    ): CursorSpecificationBuilder<T, C> {
        this.sortProperty = property
        return this.direction(direction)
    }

    /**
     * Sets the property to use for cursor-based sorting. Note: This method will automatically set
     * the cursor extractor to use the property getter.
     */
    fun cursorProperty(property: KProperty1<T, C?>): CursorSpecificationBuilder<T, C> {
        this.cursorProperty = property
        if (this.sortProperty == null) {
            this.sortBy(property)
        }
        this.cursorExtractor { entity -> property.get(entity) }
        return this
    }

    /** Sets the sort direction. */
    fun direction(direction: Sort.Direction): CursorSpecificationBuilder<T, C> {
        this.direction = direction
        return this
    }

    /** Sets additional sorting criteria beyond the primary cursor property. */
    fun additionalSort(sort: Sort): CursorSpecificationBuilder<T, C> {
        this.additionalSort = sort
        return this
    }

    /** Sets the specification for filtering entities. */
    fun specification(spec: Specification<T>): CursorSpecificationBuilder<T, C> {
        this.specification = spec
        return this
    }

    /**
     * Sets the function for extracting cursor values from entities. This is required if the cursor
     * property is not directly accessible from the entity.
     */
    fun cursorExtractor(extractor: (T) -> C?): CursorSpecificationBuilder<T, C> {
        this.cursorExtractor = extractor
        return this
    }

    /** Builds the cursor specification. */
    fun build(): CursorSpecification<T, C> {
        requireNotNull(sortProperty) { "Sort property must be set" }
        requireNotNull(cursorProperty) { "Cursor property must be set" }
        requireNotNull(cursorExtractor) { "Cursor extractor must be set" }

        val finalSpecification = specification ?: Specification<T> { _, _, _ -> null }
        val finalSortProperty = sortProperty!!
        val finalCursorProperty = cursorProperty!!
        val finalDirection = direction
        val finalCursorExtractor = cursorExtractor!!
        val finalCustomCursorPredicate = customCursorPredicate

        return object : CursorSpecification<T, C> {
            override fun getCursorProperty(): KProperty1<T, C?> = finalCursorProperty

            override fun getSortProperty(): KProperty1<T, *> = finalSortProperty

            override fun getDirection(): Sort.Direction = finalDirection

            override fun toPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                criteriaBuilder: CriteriaBuilder,
            ): Predicate =
                finalSpecification.toPredicate(root, query, criteriaBuilder)
                    ?: criteriaBuilder.conjunction()

            override fun toCursorPredicate(
                root: Root<T>,
                query: CriteriaQuery<*>,
                criteriaBuilder: CriteriaBuilder,
                cursor: C?,
            ): Predicate? {
                if (cursor == null) return null

                // If custom cursor predicate is provided, use it
                if (finalCustomCursorPredicate != null) {
                    return finalCustomCursorPredicate.invoke(root, query, criteriaBuilder, cursor)
                }

                val path = root.get<C>(finalCursorProperty.name)

                return when (finalDirection) {
                    Sort.Direction.ASC -> criteriaBuilder.greaterThanOrEqualTo(path, cursor)
                    Sort.Direction.DESC -> criteriaBuilder.lessThanOrEqualTo(path, cursor)
                }
            }

            override fun getSort(): Sort {
                val primarySort = Sort.by(finalDirection, finalSortProperty.name)
                return if (additionalSort.isSorted) {
                    primarySort.and(additionalSort)
                } else {
                    primarySort
                }
            }

            override fun extractCursor(entity: T): C? = finalCursorExtractor(entity)
        }
    }

    /**
     * Legacy method for backward compatibility. Sets both sort and cursor properties to the same
     * value.
     */
    @Deprecated(
        "Use cursorProperty() instead for better control",
        ReplaceWith("cursorProperty(property)"),
    )
    fun property(property: KProperty1<T, C?>): CursorSpecificationBuilder<T, C> {
        return cursorProperty(property)
    }
}

/** Repository interface with cursor-based pagination support. */
@NoRepositoryBean
interface CursorPagingRepository<T, ID : Serializable> :
    JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Finds entities using cursor-based pagination.
     *
     * @param C the cursor type
     * @param cursorSpec the cursor specification
     * @param cursor the starting cursor value
     * @param pageSize the number of items per page
     * @return a page of entities with cursor information
     */
    fun <C : Comparable<C>> findAllWithCursor(
        cursorSpec: CursorSpecification<T, C>,
        cursor: C?,
        pageSize: Int,
    ): CursorPage<T, C>
}

/** Default implementation of CursorPagingRepository. */
class CursorPagingRepositoryImpl<T, ID : Serializable>(
    entityInformation: JpaEntityInformation<T, ID>,
    entityManager: EntityManager,
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), CursorPagingRepository<T, ID> {

    @PersistenceContext private val em: EntityManager = entityManager

    override fun <C : Comparable<C>> findAllWithCursor(
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

        // Create query
        val query = em.createQuery(cq)
        query.maxResults = pageSize + 1 // Fetch one extra to check if there's a next page

        // Execute query
        val results = query.resultList

        // Check if there's a next page
        val hasNext = results.size > pageSize
        val content = if (hasNext) results.subList(0, pageSize) else results

        // Get previous page info
        val (hasPrevious, previousCursor) = fetchPreviousPageInfo(content, cursor, cursorSpec, cb)

        // Build page info
        val pageInfo =
            CursorPageInfo(
                cursor =
                    if (content.isNotEmpty()) cursorSpec.extractCursor(content.first()) else null,
                pageSize = content.size,
                hasPrevious = hasPrevious,
                previousCursor = previousCursor,
                hasNext = hasNext,
                nextCursor =
                    if (hasNext && content.isNotEmpty()) cursorSpec.extractCursor(results[pageSize])
                    else null,
            )

        return CursorPage(content, pageInfo)
    }

    /** Fetches information about the previous page. */
    private fun <C : Comparable<C>> fetchPreviousPageInfo(
        currentPageData: List<T>,
        cursor: C?,
        cursorSpec: CursorSpecification<T, C>,
        cb: CriteriaBuilder,
    ): Pair<Boolean, C?> {
        if (currentPageData.isEmpty() || cursor == null) {
            return Pair(false, null)
        }

        // Create reverse query
        val cq = cb.createQuery(domainClass)
        val root = cq.from(domainClass)

        // Apply base conditions
        val predicate = cursorSpec.toPredicate(root, cq, cb)

        // Apply reverse cursor conditions
        val propertyPath = root.get<C>(cursorSpec.getCursorProperty().name)
        val reverseCursorPredicate =
            when (cursorSpec.getDirection()) {
                Sort.Direction.ASC -> cb.lessThan(propertyPath, cursor)
                Sort.Direction.DESC -> cb.greaterThan(propertyPath, cursor)
            }

        // Combine conditions
        cq.where(cb.and(predicate, reverseCursorPredicate))

        // Reverse sort
        val reverseDirection =
            if (cursorSpec.getDirection() == Sort.Direction.ASC) Sort.Direction.DESC
            else Sort.Direction.ASC

        val reverseSort = Sort.by(reverseDirection, cursorSpec.getSortProperty().name)
        cq.orderBy(toJpaOrders(reverseSort, root, cb))

        // Create query
        val query = em.createQuery(cq)
        query.maxResults = 1 // Only need the last record of the previous page

        // Execute query
        val result = query.resultList

        return if (result.isNotEmpty()) {
            Pair(true, cursorSpec.extractCursor(result.first()))
        } else {
            Pair(false, null)
        }
    }

    /** Converts Spring Data Sort to JPA Order list. */
    private fun toJpaOrders(
        sort: Sort,
        root: Root<T>,
        cb: CriteriaBuilder,
    ): List<jakarta.persistence.criteria.Order> {
        return sort
            .map { order ->
                if (order.isAscending) cb.asc(root.get<Any>(order.property))
                else cb.desc(root.get<Any>(order.property))
            }
            .toList()
    }
}

/**
 * Extension function to create a cursor specification builder.
 *
 * @param T the entity type
 * @param ID the entity ID type
 * @param C the cursor type
 * @param property the property to use for cursor-based sorting
 * @return a new cursor specification builder
 */
fun <T, ID : Serializable, C : Comparable<C>> JpaRepository<T, ID>.cursorSpec(
    property: KProperty1<T, C?>
): CursorSpecificationBuilder<T, C> {
    return CursorSpecificationBuilder<T, C>().cursorProperty(property)
}

fun SortDirection.toJpaDirection(): Sort.Direction {
    return when (this) {
        SortDirection.ASCENDING -> Sort.Direction.ASC
        SortDirection.DESCENDING -> Sort.Direction.DESC
    }
}

fun CursorPageInfo<IdType>.toPageDTO(): PageDTO {
    return PageDTO(
        pageStart = cursor ?: 0,
        pageSize = pageSize,
        hasPrev = hasPrevious,
        hasMore = hasNext,
        prevStart = previousCursor,
        nextStart = nextCursor,
    )
}
