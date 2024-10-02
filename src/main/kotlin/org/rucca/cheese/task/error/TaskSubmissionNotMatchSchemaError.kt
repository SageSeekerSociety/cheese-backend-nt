package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class TaskSubmissionNotMatchSchemaError() :
        BaseError(
                HttpStatus.BAD_REQUEST,
                "Task submission does not match schema",
        )
