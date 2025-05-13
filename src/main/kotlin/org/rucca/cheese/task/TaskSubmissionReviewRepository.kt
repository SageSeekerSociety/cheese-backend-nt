package org.rucca.cheese.task

import java.util.*
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

interface TaskSubmissionReviewRepository : JpaRepository<TaskSubmissionReview, IdType> {
    fun findBySubmissionId(submissionId: IdType): Optional<TaskSubmissionReview>

    fun existsBySubmissionId(submissionId: IdType): Boolean
}
