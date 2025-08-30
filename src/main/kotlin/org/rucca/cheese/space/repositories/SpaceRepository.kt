package org.rucca.cheese.space.repositories

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.models.Space

interface SpaceRepository : CursorPagingRepository<Space, IdType> {
    fun existsByName(name: String): Boolean
}
