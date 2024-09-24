package org.rucca.cheese.team

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

enum class TeamMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
        indexes =
                [
                        Index(columnList = "team_id"),
                        Index(columnList = "user_id"),
                ])
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

    fun findAllByTeamIdAndRoleOrderByUpdatedAtDesc(
            teamId: IdType,
            role: TeamMemberRole,
            pageable: Pageable,
    ): List<TeamUserRelation>

    @Query(
            """
            SELECT team
            FROM Team team
            JOIN TeamUserRelation relation ON team.id = relation.team.id
            WHERE relation.user.id = :userId
            AND relation.role = :joinableIfRole
            AND NOT EXISTS (
                SELECT 1
                FROM TaskMembership membership
                WHERE membership.task.id = :taskId
                AND membership.memberId = team.id
            )
            """)
    fun getTeamsThatUserCanUseToJoinTask(
            taskId: IdType,
            userId: IdType,
            joinableIfRole: TeamMemberRole = TeamMemberRole.OWNER
    ): List<Team>
}
