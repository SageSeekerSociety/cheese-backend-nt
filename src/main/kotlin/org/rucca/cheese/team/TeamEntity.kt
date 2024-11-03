package org.rucca.cheese.team

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "name"),
        ]
)
@EntityListeners(TeamElasticSearchSyncListener::class)
class Team(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false) var intro: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var description: String? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var avatar: Avatar? = null,
) : BaseEntity()

interface TeamRepository : JpaRepository<Team, IdType> {
    fun existsByName(name: String): Boolean

    @Query(
        """
            SELECT team
            FROM Team team
            JOIN TeamUserRelation teamUserRelation ON team.id = teamUserRelation.team.id
            WHERE teamUserRelation.user.id = :userId
            AND (teamUserRelation.role = :ownerRole OR teamUserRelation.role = :adminRole)
            AND NOT EXISTS (
                SELECT 1
                FROM TaskMembership taskMembership
                WHERE taskMembership.task.id = :taskId
                AND taskMembership.memberId = team.id
            )
            """
    )
    fun getTeamsThatUserCanUseToJoinTask(
        taskId: IdType,
        userId: IdType,
        ownerRole: TeamMemberRole = TeamMemberRole.OWNER,
        adminRole: TeamMemberRole = TeamMemberRole.ADMIN,
    ): List<Team>

    @Query(
        """
        SELECT team
        FROM Team team
        JOIN TeamUserRelation teamUserRelation ON team.id = teamUserRelation.team.id
        JOIN TaskMembership taskMembership ON team.id = taskMembership.memberId
        WHERE teamUserRelation.user.id = :userId
        AND taskMembership.task.id = :taskId
        AND taskMembership.approved = :approved
        """
    )
    fun getTeamsThatUserCanUseToSubmitTask(
        taskId: IdType,
        userId: IdType,
        approved: ApproveType = ApproveType.APPROVED,
    ): List<Team>
}
