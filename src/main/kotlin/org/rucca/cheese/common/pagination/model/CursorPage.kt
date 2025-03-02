package org.rucca.cheese.common.pagination.model

import org.rucca.cheese.model.PageDTO

/**
 * Container for cursor-based pagination results.
 *
 * @param T The entity type
 * @param C The cursor type
 * @property content The page content
 * @property pageInfo Pagination metadata
 */
data class CursorPage<T, C : Cursor<T>>(val content: List<T>, val pageInfo: CursorPageInfo<T, C>)

/**
 * Pagination metadata for cursor-based navigation.
 *
 * @param C The cursor type
 * @property cursor Current cursor (typically points to the first item)
 * @property pageSize Number of items in the page
 * @property hasPrevious Whether a previous page exists
 * @property previousCursor Cursor for the previous page
 * @property hasNext Whether a next page exists
 * @property nextCursor Cursor for the next page
 */
data class CursorPageInfo<T, C : Cursor<T>>(
    val cursor: C? = null,
    val pageSize: Int,
    val hasPrevious: Boolean = false,
    val previousCursor: C? = null,
    val hasNext: Boolean = false,
    val nextCursor: C? = null,
)

/**
 * Convert CursorPageInfo to legacy PageDTO for backward compatibility.
 *
 * @return A PageDTO with equivalent pagination information
 */
fun <T, V : Number, C : SimpleCursor<T, V>> CursorPageInfo<T, C>.toPageDTO(): PageDTO {
    return PageDTO(
        pageStart = cursor?.value?.toLong() ?: 0,
        pageSize = pageSize,
        hasPrev = hasPrevious,
        hasMore = hasNext,
        prevStart = previousCursor?.value?.toLong(),
        nextStart = nextCursor?.value?.toLong(),
    )
}
