package org.rucca.cheese.common.pagination.spec

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.rucca.cheese.common.pagination.model.Cursor
import org.springframework.data.domain.Sort

/**
 * Interface for cursor-based pagination specifications.
 *
 * This interface defines the contract for creating database queries that support cursor-based
 * pagination and sorting.
 *
 * @param T The entity type
 * @param C The cursor type
 */
interface CursorSpecification<T, C : Cursor<T>> {
    /**
     * Creates the base predicate for filtering entities.
     *
     * @param root JPA criteria root
     * @param query JPA criteria query
     * @param criteriaBuilder JPA criteria builder
     * @return Base predicate for filtering entities
     */
    fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
    ): Predicate

    /**
     * Creates cursor-based predicate for pagination.
     *
     * @param root JPA criteria root
     * @param query JPA criteria query
     * @param criteriaBuilder JPA criteria builder
     * @param cursor Starting cursor value
     * @return Predicate for cursor-based filtering, or null if no cursor
     */
    fun toCursorPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        cursor: C?,
    ): Predicate?

    /**
     * Returns the sort specification for the query.
     *
     * @return Sort specification
     */
    fun getSort(): Sort

    /**
     * Extracts cursor value from an entity.
     *
     * @param entity Source entity
     * @return Cursor representing the entity's position, or null if unavailable
     */
    fun extractCursor(entity: T): C?
}
