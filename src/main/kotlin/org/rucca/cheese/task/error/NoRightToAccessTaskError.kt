package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class NoRightToAccessTaskError(
    taskId: IdType,
) :
    BaseError(
        HttpStatus.FORBIDDEN,
        "You do not have access to the task with id $taskId",
        mapOf("taskId" to taskId),
    )
