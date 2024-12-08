/*
 *  Description: This file defines the TaskMembership entity and its repository.
 *               It stores the information of a task's membership.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.Optional
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Embeddable
class TaskMembershipRealNameInfo(
    @Column(nullable = false) val realName: String? = null,
    @Column(nullable = false) val studentId: String? = null,
    @Column(nullable = false) val grade: String? = null,
    @Column(nullable = false) val major: String? = null,
    @Column(nullable = false) val className: String? = null,
    @Column(nullable = false) val email: String? = null,
    @Column(nullable = false) val phone: String? = null,
    @Column(nullable = false) val applyReason: String? = null,
    @Column(nullable = false) val personalAdvantage: String? = null,
    @Column(nullable = false) val remark: String? = null,
)

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "task_id"),
            Index(columnList = "member_id"),
        ]
)
class TaskMembership(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
    @Column(nullable = false) val memberId: IdType? = null,
    @Column(nullable = true) var deadline: LocalDateTime? = null,
    @Column(nullable = false) var approved: ApproveType? = null,
    @Column(nullable = false) var realNameInfo: TaskMembershipRealNameInfo? = null,
) : BaseEntity()

interface TaskMembershipRepository : JpaRepository<TaskMembership, IdType> {
    fun findAllByTaskId(taskId: IdType): List<TaskMembership>

    fun findAllByTaskIdAndApproved(taskId: IdType, approved: ApproveType): List<TaskMembership>

    fun findByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Optional<TaskMembership>

    fun existsByTaskIdAndMemberId(taskId: IdType, memberId: IdType): Boolean

    fun existsByTaskIdAndMemberIdAndApproved(
        taskId: IdType,
        memberId: IdType,
        approved: ApproveType
    ): Boolean

    fun existsByTaskId(taskId: IdType): Boolean

    fun countByTaskIdAndApproved(taskId: IdType, approved: ApproveType): Int

    @Query(
        "SELECT tm FROM TaskMembership tm WHERE tm.task.id = :taskId AND EXISTS (SELECT 1 FROM TaskSubmission ts WHERE ts.membership.id = tm.id)"
    )
    fun findByTaskIdWhereMemberHasSubmitted(taskId: IdType): List<TaskMembership>
}
