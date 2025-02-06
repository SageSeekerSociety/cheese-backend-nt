/*
 *  Description: This file defines the NotTaskParticipantYetError class.
 *               It is thrown when a user is not a participant of a task.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class NotTaskParticipantYetError(taskId: IdType, memberId: IdType) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "Member $memberId is not a participant of task $taskId",
        mapOf("taskId" to taskId, "memberId" to memberId),
    )
