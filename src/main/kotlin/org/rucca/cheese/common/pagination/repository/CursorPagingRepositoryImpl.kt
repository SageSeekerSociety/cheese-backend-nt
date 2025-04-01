package org.rucca.cheese.common.pagination.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import java.io.Serializable
import org.rucca.cheese.common.pagination.model.Cursor
import org.rucca.cheese.common.pagination.model.CursorPage
import org.rucca.cheese.common.pagination.model.CursorPageInfo
import org.rucca.cheese.common.pagination.spec.CursorSpecification
import org.rucca.cheese.common.pagination.util.JpaUtils
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
) :
    SimpleJpaRepository<T, ID>(entityInformation, entityManager),
    CursorPagingRepository<T, ID> where
ID : Serializable,
ID : Comparable<ID> { // Ensure ID is Comparable if used in cursor

    @PersistenceContext private val em: EntityManager = entityManager

    override fun <C : Cursor<T>> findAllWithCursor(
        cursorSpec: CursorSpecification<T, C>,
        cursor: C?,
        pageSize: Int,
    ): CursorPage<T, C> {
        require(pageSize > 0) { "Page size must be positive" }

        val cb = em.criteriaBuilder
        val cq = cb.createQuery(domainClass)
        val root = cq.from(domainClass)

        // Apply base conditions from Specification
        val predicate = cursorSpec.toPredicate(root, cq, cb)

        // Apply cursor conditions
        val cursorPredicate = cursorSpec.toCursorPredicate(root, cq, cb, cursor)

        // Combine predicates
        val finalPredicate =
            if (cursorPredicate != null) {
                cb.and(predicate, cursorPredicate)
            } else {
                predicate
            }
        cq.where(finalPredicate) // Apply final where clause

        // Apply sorting using potentially nested paths
        cq.orderBy(toJpaOrders(cursorSpec.getSort(), root, cb)) // Use updated helper

        // Create query with one extra result to check for next page
        val query =
            em.createQuery(cq).apply {
                // Add fetch joins or entity graphs here if needed for performance
                maxResults = pageSize + 1
            }

        // Execute query
        val results: List<T>
        try {
            results = query.resultList
        } catch (e: Exception) {
            // Log query details for debugging if needed
            // logger.error("Error executing cursor query: ${e.message}", e)
            throw RuntimeException("Failed to execute cursor query", e)
        }

        // Determine content and if there's a next page
        val hasNext = results.size > pageSize
        val content = if (hasNext) results.subList(0, pageSize) else results

        // Build page info, extracting cursors from content
        val firstEntityCursor =
            if (content.isNotEmpty()) cursorSpec.extractCursor(content.first()) else null
        val nextEntityCursor =
            if (hasNext && content.isNotEmpty()) {
                // Extract cursor from the (pageSize+1)-th element, which wasn't included in content
                cursorSpec.extractCursor(results[pageSize])
            } else null

        val pageInfo =
            CursorPageInfo(
                // The 'cursor' in PageInfo should arguably represent the cursor *for the current
                // page's first item*
                cursor = firstEntityCursor,
                pageSize = content.size,
                hasNext = hasNext,
                // The 'nextCursor' points to the start of the *next* page
                nextCursor = nextEntityCursor,
            )

        return CursorPage(content, pageInfo)
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
