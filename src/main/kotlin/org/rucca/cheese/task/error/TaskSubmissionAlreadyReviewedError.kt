/*
 *  Description: This file defines the TaskSubmissionAlreadyReviewedError class.
 *               It is thrown when a submission has already been reviewed.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskSubmissionAlreadyReviewedError(submissionId: IdType) :
    BaseError(
        HttpStatus.CONFLICT,
        "Submission with id $submissionId has already been reviewed",
        mapOf("submissionId" to submissionId),
    )
