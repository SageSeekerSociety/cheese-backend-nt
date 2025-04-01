package org.rucca.cheese.space.repositories

import java.util.*
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.space.models.SpaceAdminRelation
import org.rucca.cheese.space.models.SpaceAdminRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SpaceAdminRelationRepository : JpaRepository<SpaceAdminRelation, IdType> {
    fun findAllBySpaceId(spaceId: IdType): List<SpaceAdminRelation>

    @Query(
        "SELECT sar FROM SpaceAdminRelation sar JOIN FETCH sar.user WHERE sar.space.id = :spaceId AND sar.deletedAt IS NULL"
    )
    fun findAllBySpaceIdFetchUser(spaceId: IdType): List<SpaceAdminRelation>

    fun findBySpaceIdAndRole(spaceId: IdType, role: SpaceAdminRole): Optional<SpaceAdminRelation>

    fun findBySpaceIdAndUserId(spaceId: IdType, userId: IdType): Optional<SpaceAdminRelation>

    fun existsBySpaceIdAndUserId(spaceId: IdType, userId: IdType): Boolean

    fun existsBySpaceIdAndUserIdAndRole(
        spaceId: IdType,
        userId: IdType,
        role: SpaceAdminRole,
    ): Boolean
}
