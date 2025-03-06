package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: NONE,PENDING,PROCESSING,COMPLETED,FAILED */
enum class TaskAIAdviceGenerationStatusDTO(@get:JsonValue val value: kotlin.String) {

    NONE("NONE"),
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TaskAIAdviceGenerationStatusDTO {
            return values().first { it -> it.value == value }
        }
    }
}
