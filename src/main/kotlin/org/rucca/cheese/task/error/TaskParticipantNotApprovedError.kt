/*
 *  Description: This file defines the TaskParticipantNotApprovedError class.
 *               It is thrown when a user's participation in a task has not been approved.
 *
 *  Author(s):
 *      nameisyui
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskParticipantNotApprovedError(taskId: IdType, memberId: IdType) :
    BaseError(
        HttpStatus.FORBIDDEN,
        "Member ${memberId}'s participation in task ${taskId} has not been approved.",
        mapOf("taskId" to taskId, "memberId" to memberId),
    )
