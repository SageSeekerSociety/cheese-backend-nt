package org.rucca.cheese.common.pagination.spec

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import kotlin.reflect.KProperty1
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

/**
 * Builder for creating ID-based seek specifications.
 *
 * @param T The entity type
 * @param ID The ID type
 * @param entityFinder Function to find an entity by its ID
 */
class IdSeekSpecificationBuilder<T, ID : Comparable<ID>, P : Comparable<P>>(
    private val idProperty: KProperty1<T, ID?>,
    private val sortProperty: KProperty1<T, P>,
    private var direction: Sort.Direction = Sort.Direction.DESC,
    private val entityFinder: (ID) -> T?,
) {
    private var specification: Specification<T>? = null

    /** Set the sort direction. */
    fun direction(direction: Sort.Direction): IdSeekSpecificationBuilder<T, ID, P> {
        this.direction = direction
        return this
    }

    /** Set a filter specification. */
    fun specification(spec: Specification<T>): IdSeekSpecificationBuilder<T, ID, P> {
        this.specification = spec
        return this
    }

    /** Set a filter specification using a lambda. */
    fun specification(
        specFn: (Root<T>, CriteriaQuery<*>, CriteriaBuilder) -> Predicate?
    ): IdSeekSpecificationBuilder<T, ID, P> {
        this.specification = Specification { root, query, cb -> specFn(root, query!!, cb) }
        return this
    }

    /**
     * Build the ID-based seek specification.
     *
     * @return A new ID-based seek specification
     * @throws IllegalArgumentException if sort property is not set
     */
    @Suppress("UNCHECKED_CAST")
    fun build(): IdSeekSpecification<T, ID, P> {
        val property = sortProperty

        return IdSeekSpecification(
            idProperty = idProperty,
            sortProperty = property,
            direction = direction,
            referenceEntityFinder = entityFinder,
            additionalSpecification = specification,
        )
    }
}
