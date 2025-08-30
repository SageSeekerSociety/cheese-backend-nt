package org.rucca.cheese.common.pagination.repository

import java.io.Serializable
import org.rucca.cheese.common.pagination.model.Cursor
import org.rucca.cheese.common.pagination.model.CursorPage
import org.rucca.cheese.common.pagination.spec.CursorSpecification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean

/**
 * Repository interface with cursor-based pagination support.
 *
 * @param T The entity type
 * @param ID The entity ID type
 */
@NoRepositoryBean
interface CursorPagingRepository<T, ID> : JpaRepository<T, ID>, JpaSpecificationExecutor<T> where
ID : Serializable,
ID : Comparable<ID> {

    /**
     * Find entities using cursor-based pagination.
     *
     * @param C The cursor type
     * @param cursorSpec The cursor specification
     * @param cursor The starting cursor (null for first page)
     * @param pageSize The number of items per page
     * @return A page of entities with cursor information
     *
     * Example usage:
     * ```
     * // Simple cursor - paginate by id
     * val simpleSpec = repository.simpleCursorSpec(Entity::id)
     *     .sortBy(Entity::createdAt, Sort.Direction.DESC)
     *     .specification { root, _, cb -> cb.equal(root.get("active"), true) }
     *     .build()
     *
     * // Get first page
     * val firstPage = repository.findAllWithCursor(simpleSpec, null, 20)
     *
     * // Get next page using cursor
     * val nextCursor = firstPage.pageInfo.nextCursor
     * val secondPage = repository.findAllWithCursor(simpleSpec, nextCursor, 20)
     *
     * // Composite cursor - multiple properties
     * val compositeSpec = repository.compositeCursorSpec(Entity::status, Entity::priority, Entity::id)
     *     .sortBy(
     *         Entity::status to Sort.Direction.ASC,
     *         Entity::priority to Sort.Direction.DESC,
     *         Entity::id to Sort.Direction.ASC
     *     )
     *     .build()
     *
     * // Access results
     * val items = page.content
     * val hasNext = page.pageInfo.hasNext
     * val encodedCursor = page.pageInfo.nextCursor?.encode() // For client transmission
     * ```
     */
    fun <C : Cursor<T>> findAllWithCursor(
        cursorSpec: CursorSpecification<T, C>,
        cursor: C? = null,
        pageSize: Int,
    ): CursorPage<T, C>
}
