package org.rucca.cheese.space.repositories

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.models.SpaceCategory

interface SpaceCategoryRepository : CursorPagingRepository<SpaceCategory, IdType> {
    // Find active (not archived) categories
    fun findBySpaceIdAndArchivedAtIsNullOrderByDisplayOrderAscNameAsc(
        spaceId: IdType
    ): List<SpaceCategory>

    // Find all categories (including archived)
    fun findBySpaceIdOrderByDisplayOrderAscNameAsc(spaceId: IdType): List<SpaceCategory>

    // Check for active name uniqueness
    fun existsBySpaceIdAndNameAndArchivedAtIsNull(spaceId: IdType, name: String): Boolean

    // Find specific active category
    fun findByIdAndSpaceIdAndArchivedAtIsNull(id: IdType, spaceId: IdType): SpaceCategory?

    // Find specific category regardless of archive status
    fun findByIdAndSpaceId(id: IdType, spaceId: IdType): SpaceCategory?

    // Find all by space ID (for cascade checks/updates)
    fun findAllBySpaceId(spaceId: IdType): List<SpaceCategory>

    fun findBySpaceIdAndNameAndArchivedAtIsNull(spaceId: IdType, name: String): SpaceCategory?
}
