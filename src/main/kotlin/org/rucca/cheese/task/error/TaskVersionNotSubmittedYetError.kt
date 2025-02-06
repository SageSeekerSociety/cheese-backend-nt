/*
 *  Description: This file defines the TaskVersionNotSubmittedYetError class.
 *               It is thrown when a version of a task has not been submitted yet.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskVersionNotSubmittedYetError(taskId: IdType, memberId: IdType, versionId: Int) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "Member $memberId has not submitted $versionId version of task $taskId yet",
        mapOf("taskId" to taskId, "memberId" to memberId, "versionId" to versionId),
    )
