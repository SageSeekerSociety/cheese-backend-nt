package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskSubmissionNotReviewedYetError(submissionId: IdType) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "Submission with id $submissionId has not been reviewed yet",
        mapOf("submissionId" to submissionId)
    )
