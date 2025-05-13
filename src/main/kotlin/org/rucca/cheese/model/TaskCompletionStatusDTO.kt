package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: NOT_SUBMITTED,PENDING_REVIEW,REJECTED_RESUBMITTABLE,SUCCESS,FAILED */
enum class TaskCompletionStatusDTO(@get:JsonValue val value: kotlin.String) {

    NOT_SUBMITTED("NOT_SUBMITTED"),
    PENDING_REVIEW("PENDING_REVIEW"),
    REJECTED_RESUBMITTABLE("REJECTED_RESUBMITTABLE"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TaskCompletionStatusDTO {
            return values().first { it -> it.value == value }
        }
    }
}
