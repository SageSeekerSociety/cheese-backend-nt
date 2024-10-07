package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: USER,TEAM */
enum class TaskSubmitterTypeDTO(@get:JsonValue val value: kotlin.String) {

    USER("USER"),
    TEAM("TEAM");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TaskSubmitterTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
