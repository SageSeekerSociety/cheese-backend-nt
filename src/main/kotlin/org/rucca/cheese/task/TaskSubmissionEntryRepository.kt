package org.rucca.cheese.task

import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

interface TaskSubmissionEntryRepository : JpaRepository<TaskSubmissionEntry, IdType> {
    fun findAllByTaskSubmissionId(taskSubmissionId: IdType): List<TaskSubmissionEntry>
}
