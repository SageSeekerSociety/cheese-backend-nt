/*
 *  Description: This file defines the TeamUserRelation entity and its repository.
 *               It stores the relationship between a team and a user.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.team

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TeamMemberRoleTypeDTO
import org.rucca.cheese.user.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

enum class TeamMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
}

fun TeamMemberRoleTypeDTO?.toTeamMemberRole(): TeamMemberRole {
    return when (this) {
        TeamMemberRoleTypeDTO.OWNER ->
            TeamMemberRole.OWNER // Should not be directly invited as owner usually
        TeamMemberRoleTypeDTO.ADMIN -> TeamMemberRole.ADMIN
        TeamMemberRoleTypeDTO.MEMBER -> TeamMemberRole.MEMBER
        null -> TeamMemberRole.MEMBER // Default to MEMBER if null
    }
}

fun TeamMemberRole.toDTO(): TeamMemberRoleTypeDTO {
    return when (this) {
        TeamMemberRole.OWNER -> TeamMemberRoleTypeDTO.OWNER
        TeamMemberRole.ADMIN -> TeamMemberRoleTypeDTO.ADMIN
        TeamMemberRole.MEMBER -> TeamMemberRoleTypeDTO.MEMBER
    }
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "team_id"), Index(columnList = "user_id")])
class TeamUserRelation(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val user: User? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val team: Team? = null,
    @Column(nullable = false) var role: TeamMemberRole? = null,
) : BaseEntity()

interface TeamUserRelationRepository : JpaRepository<TeamUserRelation, IdType> {
    fun findByTeamIdAndRole(teamId: IdType, role: TeamMemberRole): Optional<TeamUserRelation>

    fun findByTeamIdAndUserId(teamId: IdType, userId: IdType): Optional<TeamUserRelation>

    fun findAllByTeamId(teamId: IdType): List<TeamUserRelation>

    fun findAllByUserId(userId: IdType): List<TeamUserRelation>

    fun countByTeamIdAndRole(teamId: IdType, role: TeamMemberRole): Int

    fun countByTeamIdAndDeletedAtIsNull(teamId: IdType): Int

    fun findAllByTeamIdAndRoleOrderByUpdatedAtDesc(
        teamId: IdType,
        role: TeamMemberRole,
        pageable: Pageable,
    ): List<TeamUserRelation>

    fun findAllByTeamIdAndRoleIsIn(
        teamId: Long,
        roles: Collection<TeamMemberRole>,
    ): List<TeamUserRelation>

    fun findAllByTeamIdInAndUserId(teamIds: Collection<Long>, userId: Long): List<TeamUserRelation>
}
