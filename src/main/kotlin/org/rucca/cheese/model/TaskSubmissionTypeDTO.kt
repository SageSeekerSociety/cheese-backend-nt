package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: TEXT,FILE */
enum class TaskSubmissionTypeDTO(@get:JsonValue val value: kotlin.String) {

    TEXT("TEXT"),
    FILE("FILE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TaskSubmissionTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
