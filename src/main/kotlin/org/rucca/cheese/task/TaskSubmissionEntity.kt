/*
 *  Description: This file defines the TaskSubmission entity and its repository.
 *               It stores the information of a task submission.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import java.util.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.repository.CursorPagingRepository
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "membership_id")])
class TaskSubmission(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val membership: TaskMembership? = null,
    @Column(nullable = false) val version: Int? = null,
    @JoinColumn(name = "submitter_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val submitter: User? = null,
) : BaseEntity()

interface TaskSubmissionRepository : CursorPagingRepository<TaskSubmission, IdType> {
    fun findAllByMembershipId(membershipId: IdType): List<TaskSubmission>

    fun findAllByMembershipIdAndVersion(membershipId: IdType, version: Int): List<TaskSubmission>

    @Query("SELECT MAX(ts.version) FROM TaskSubmission ts WHERE ts.membership.id = :membershipId")
    fun findVersionNumberByMembershipId(membershipId: IdType): Optional<Int>

    @Query(
        "SELECT count(submission) > 0 FROM TaskSubmission submission JOIN submission.membership membership WHERE membership.task.id = :taskId"
    )
    fun existsByTaskId(taskId: IdType): Boolean
}
