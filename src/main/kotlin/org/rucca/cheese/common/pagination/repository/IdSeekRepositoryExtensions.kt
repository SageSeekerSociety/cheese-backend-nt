package org.rucca.cheese.common.pagination.repository

import org.rucca.cheese.common.pagination.model.CursorPage
import org.rucca.cheese.common.pagination.model.SimpleCursor
import org.rucca.cheese.common.pagination.spec.IdSeekSpecification
import org.rucca.cheese.common.pagination.spec.IdSeekSpecificationBuilder
import org.springframework.data.domain.Sort
import java.io.Serializable
import kotlin.reflect.KProperty1

/**
 * Extension function to create an ID-based seek specification builder that supports nullable ID
 * properties.
 *
 * @param T The entity type
 * @param ID The ID type (must be Comparable)
 * @param idProperty The entity's ID property (can be nullable)
 * @return A new ID-based seek specification builder
 */
fun <T, ID, P : Comparable<P>> CursorPagingRepository<T, ID>.idSeekSpec(
    idProperty: KProperty1<T, ID?>,
    sortProperty: KProperty1<T, P>,
    direction: Sort.Direction = Sort.Direction.DESC,
): IdSeekSpecificationBuilder<T, ID, P> where ID : Serializable, ID : Comparable<ID> {

    // Use the repository's findById method as the entity finder
    val entityFinder: (ID) -> T? = { id -> this.findById(id).orElse(null) }

    return IdSeekSpecificationBuilder(
        idProperty = idProperty,
        sortProperty = sortProperty,
        direction = direction,
        entityFinder = entityFinder,
    )
}

/**
 * Find entities using nullable ID-based seek pagination.
 *
 * @param T The entity type
 * @param ID The ID type
 * @param P The sort property type
 * @param spec The nullable ID-based seek specification
 * @param cursorValue The ID value to use as cursor (can be null)
 * @param pageSize The number of items per page
 * @return A page of entities with cursor information
 */
fun <T, ID, P : Comparable<P>> CursorPagingRepository<T, ID>.findAllWithIdCursor(
    spec: IdSeekSpecification<T, ID, P>,
    cursorValue: ID?,
    pageSize: Int,
): CursorPage<T, SimpleCursor<T, ID>> where ID : Comparable<ID>, ID : Serializable {
    // Convert ID to cursor
    val cursor = cursorValue?.let { SimpleCursor<T, ID>(it) }

    // Execute query
    return findAllWithCursor(spec, cursor, pageSize)
}

/** Convenience method for nullable ID properties. */
@Suppress("UNCHECKED_CAST")
fun <T, ID, P : Comparable<P>> CursorPagingRepository<T, ID>.findAllWithIdSeek(
    idProperty: KProperty1<T, ID?>,
    sortProperty: KProperty1<T, P>,
    direction: Sort.Direction = Sort.Direction.DESC,
    cursorValue: ID? = null,
    pageSize: Int = 20,
): CursorPage<T, SimpleCursor<T, ID>> where ID : Serializable, ID : Comparable<ID> {

    // Create spec
    val spec = idSeekSpec(idProperty, sortProperty, direction).build()

    // Execute query
    return findAllWithIdCursor(spec, cursorValue, pageSize)
}
