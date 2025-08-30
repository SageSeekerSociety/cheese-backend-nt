package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class EmailOrPhoneRequiredError(taskId: IdType, memberId: IdType) :
    BaseError(
        HttpStatus.BAD_REQUEST,
        "Either email or phone number is required for task assignment.",
        mapOf("taskId" to taskId, "memberId" to memberId),
    )
