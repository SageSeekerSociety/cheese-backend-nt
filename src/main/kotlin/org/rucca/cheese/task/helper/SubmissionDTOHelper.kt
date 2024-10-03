package org.rucca.cheese.task.helper

import org.rucca.cheese.model.PostTaskSubmissionRequestInnerDTO
import org.rucca.cheese.task.TaskService

fun List<PostTaskSubmissionRequestInnerDTO>.toEntryList() =
    this.map {
        if (it.contentText != null) {
            TaskService.TaskSubmissionEntry.Text(it.contentText)
        } else if (it.contentAttachmentId != null) {
            TaskService.TaskSubmissionEntry.Attachment(it.contentAttachmentId)
        } else {
            throw IllegalArgumentException("Invalid PostTaskSubmissionRequestInnerDTO: $it")
        }
    }
