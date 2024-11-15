package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskParticipantsReachedLimitError(
    taskId: IdType,
    limit: Int,
    actual: Int,
) :
    BaseError(
        HttpStatus.FORBIDDEN,
        "Task $taskId has a limitation of $limit for approved participants, and it already has $actual approved participants.",
        mapOf(
            "taskId" to taskId,
            "limit" to limit,
            "actual" to actual,
        ),
    )
