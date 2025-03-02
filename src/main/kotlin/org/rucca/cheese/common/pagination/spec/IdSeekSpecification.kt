package org.rucca.cheese.common.pagination.spec

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.rucca.cheese.common.pagination.model.SimpleCursor
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import kotlin.reflect.KProperty1

/**
 * Cursor specification that uses ID as cursor but performs seek-style pagination based on a
 * specified sort property.
 *
 * @param T The entity type
 * @param ID The ID type (must be Comparable)
 * @param P The sort property type (must be Comparable)
 */
class IdSeekSpecification<T, ID : Comparable<ID>, P : Comparable<P>>(
    private val idProperty: KProperty1<T, ID?>,
    private val sortProperty: KProperty1<T, P>,
    private val direction: Sort.Direction,
    private val referenceEntityFinder: (ID) -> T?,
    private val additionalSpecification: Specification<T>?,
) : CursorSpecification<T, SimpleCursor<T, ID>> {

    override fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
    ): Predicate {
        return additionalSpecification?.toPredicate(root, query, criteriaBuilder)
            ?: criteriaBuilder.conjunction()
    }

    override fun toCursorPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        cursor: SimpleCursor<T, ID>?,
    ): Predicate? {
        // If no cursor, return no predicate (start from beginning)
        if (cursor == null) return null

        val cursorId = cursor.value

        // Find the referenced entity to get its sortProperty value
        val referencedEntity =
            referenceEntityFinder(cursorId)
                ?: return criteriaBuilder.equal(root.get<ID>(idProperty.name), cursorId)

        // Get the sort value from the referenced entity
        val sortValue = sortProperty.get(referencedEntity)

        // Create path expressions
        val idPath = root.get<ID>(idProperty.name)
        val sortPath = root.get<P>(sortProperty.name)

        // Create the composite seek predicate based on direction
        return when (direction) {
            Sort.Direction.ASC -> {
                criteriaBuilder.or(
                    criteriaBuilder.greaterThanOrEqualTo(sortPath, sortValue),
                    criteriaBuilder.and(
                        criteriaBuilder.equal(sortPath, sortValue),
                        criteriaBuilder.greaterThanOrEqualTo(idPath, cursorId),
                    ),
                )
            }
            Sort.Direction.DESC -> {
                criteriaBuilder.or(
                    criteriaBuilder.lessThanOrEqualTo(sortPath, sortValue),
                    criteriaBuilder.and(
                        criteriaBuilder.equal(sortPath, sortValue),
                        criteriaBuilder.greaterThanOrEqualTo(idPath, cursorId),
                    ),
                )
            }
        }
    }

    override fun getSort(): Sort {
        // Primary sort by the sort property
        val primarySort = Sort.by(direction, sortProperty.name)

        // Always add ID as secondary sort for stable pagination
        val secondarySort = Sort.by(Sort.Direction.ASC, idProperty.name)

        return primarySort.and(secondarySort)
    }

    override fun extractCursor(entity: T): SimpleCursor<T, ID>? {
        val id = idProperty.get(entity)
        return if (id != null) SimpleCursor(id) else null
    }
}
