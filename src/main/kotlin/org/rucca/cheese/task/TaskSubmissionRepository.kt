package org.rucca.cheese.task

import java.util.*
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.Query

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
