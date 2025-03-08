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
     * // Simple example - sort by creation date
     * val spec = repository.cursorSpec()
     *     .sortBy(User::createdAt, Sort.Direction.DESC)
     *     .build()
     *
     * // Complex example - multiple sort properties with filtering
     * val spec = repository.cursorSpec()
     *     .sortBy(
     *         User::status asc(),
     *         User::priority desc(),
     *         User::id asc()
     *     )
     *     .specification { root, _, cb ->
     *         cb.equal(root.get("active"), true)
     *     }
     *     .build()
     *
     * // Execute the query
     * val page = repository.findAllWithCursor(spec, startCursor, 20)
     *
     * // Access results
     * val items = page.content
     * val nextCursor = page.pageInfo.nextCursor?.encode()
     * ```
     */
    fun <C : Cursor<T>> findAllWithCursor(
        cursorSpec: CursorSpecification<T, C>,
        cursor: C? = null,
        pageSize: Int,
    ): CursorPage<T, C>
}
