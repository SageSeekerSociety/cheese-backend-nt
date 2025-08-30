package org.rucca.cheese.space.repositories

import java.util.*
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.models.SpaceUserRank
import org.springframework.data.jpa.repository.JpaRepository

interface SpaceUserRankRepository : JpaRepository<SpaceUserRank, IdType> {
    fun findBySpaceIdAndUserId(spaceId: IdType, userId: IdType): Optional<SpaceUserRank>
}
