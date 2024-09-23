package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class TaskSubmissionNotEditableError(taskId: IdType) :
        BaseError(
                HttpStatus.BAD_REQUEST,
                "Submission of task with id $taskId is not editable",
                mapOf("taskId" to taskId))