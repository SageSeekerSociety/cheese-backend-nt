/*
 *  Description: This file defines the SpaceAdminRelation entity and its repository.
 *               It stores the relationship between a space and its admin.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.space

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

enum class SpaceAdminRole {
    OWNER,
    ADMIN,
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "space_id"), Index(columnList = "user_id")])
class SpaceAdminRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val space: Space? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val user: User? = null,
    @Column(nullable = false) var role: SpaceAdminRole? = null,
) : BaseEntity()

interface SpaceAdminRelationRepository : JpaRepository<SpaceAdminRelation, IdType> {
    fun findAllBySpaceId(spaceId: IdType): List<SpaceAdminRelation>

    @Query("SELECT sar FROM SpaceAdminRelation sar JOIN FETCH sar.user WHERE sar.space.id = :spaceId AND sar.deletedAt IS NULL")
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
