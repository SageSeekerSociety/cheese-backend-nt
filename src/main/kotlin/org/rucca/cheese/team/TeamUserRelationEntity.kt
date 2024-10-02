package org.rucca.cheese.team

import jakarta.persistence.*
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

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
}
