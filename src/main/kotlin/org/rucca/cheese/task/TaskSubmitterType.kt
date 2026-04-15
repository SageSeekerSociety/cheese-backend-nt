package org.rucca.cheese.task

import org.rucca.cheese.model.TaskSubmitterTypeDTO

enum class TaskSubmitterType {
    USER,
    TEAM,
}

fun TaskSubmitterType.toDTO(): TaskSubmitterTypeDTO {
    return when (this) {
        TaskSubmitterType.USER -> TaskSubmitterTypeDTO.USER
        TaskSubmitterType.TEAM -> TaskSubmitterTypeDTO.TEAM
    }
}
