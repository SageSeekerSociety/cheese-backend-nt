package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class AlreadyBeTaskParticipantError(
    taskId: IdType,
    memberId: IdType,
) :
    BaseError(
        HttpStatus.CONFLICT,
        "Member $memberId is already a participant of task $taskId",
        mapOf(
            "taskId" to taskId,
            "memberId" to memberId,
        ),
    )
