package org.rucca.cheese.task

import java.time.LocalDateTime
import java.util.*
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TaskMembershipRepository : JpaRepository<TaskMembership, IdType> {
    fun findAllByTaskId(taskId: IdType): List<TaskMembership>

    fun findAllByTaskIdAndApproved(taskId: IdType, approved: ApproveType): List<TaskMembership>

    fun findByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Optional<TaskMembership>

    fun existsByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Boolean

    fun existsByTaskIdAndMemberIdAndApproved(
        taskId: IdType,
        memberId: IdType,
        approved: ApproveType,
    ): Boolean

    fun existsByTaskId(taskId: IdType): Boolean

    fun countByTaskIdAndApproved(taskId: IdType, approved: ApproveType): Int

    @Query(
        "SELECT tm FROM TaskMembership tm WHERE tm.task.id = :taskId AND EXISTS (SELECT 1 FROM TaskSubmission ts WHERE ts.membership.id = tm.id)"
    )
    fun findByTaskIdWhereMemberHasSubmitted(taskId: IdType): List<TaskMembership>

    fun findAllByIsTeam(isTeam: Boolean, pageable: Pageable): Page<TaskMembership>

    /**
     * Finds TaskMemberships for a specific team that are currently imposing a lock on team
     * membership changes, based on the task's locking policy and the participation status not being
     * concluded.
     *
     * @param teamId The ID of the team.
     * @param approvedStatus The status indicating active participation (e.g., APPROVED).
     * @param lockingPolicies The list of Task locking policies that trigger a lock.
     * @param ongoingStatuses The list of TaskCompletionStatus values considered 'ongoing' (e.g.,
     *   not SUCCESS or FAILED).
     * @param currentTime The current time, used for checking membership deadlines.
     * @return A list of TaskMemberships indicating an active lock.
     */
    @Query(
        """
    SELECT tm
    FROM TaskMembership tm
    JOIN FETCH tm.task t
    WHERE tm.memberId = :teamId
      AND tm.isTeam = true
      AND tm.approved = :approvedStatus
      AND (tm.deadline IS NULL OR tm.deadline > :currentTime)
      AND t.teamLockingPolicy IN :lockingPolicies
      AND tm.completionStatus IN :ongoingStatuses
      AND tm.deletedAt IS NULL
      AND t.deletedAt IS NULL 
    """
    )
    fun findActiveMembershipsWithOngoingLock(
        teamId: IdType,
        approvedStatus: ApproveType,
        lockingPolicies: List<TeamMembershipLockPolicy>,
        ongoingStatuses: List<TaskCompletionStatus>,
        currentTime: LocalDateTime,
    ): List<TaskMembership>

    // Find memberships with specific statuses and a non-null deadline (for scheduler)
    fun findByCompletionStatusInAndDeadlineNotNull(
        statuses: List<TaskCompletionStatus>,
        pageable: Pageable,
    ): Page<TaskMembership>
}
