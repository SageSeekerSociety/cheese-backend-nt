/*
 *  Description: This file defines the TaskNotResubmittableError class.
 *               It is thrown when a task is not resubmittable.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskNotResubmittableError(taskId: IdType) :
    BaseError(
        HttpStatus.BAD_REQUEST,
        "Task $taskId is not resubmittable",
        mapOf("taskId" to taskId)
    )
