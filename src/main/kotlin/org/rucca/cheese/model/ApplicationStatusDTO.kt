package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Current status of the application. PENDING=Waiting action, APPROVED=Request approved,
 * REJECTED=Request rejected, ACCEPTED=Invitation accepted, DECLINED=Invitation declined,
 * CANCELED=Canceled by initiator. Values: PENDING,APPROVED,REJECTED,ACCEPTED,DECLINED,CANCELED
 */
enum class ApplicationStatusDTO(@get:JsonValue val value: kotlin.String) {

    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ACCEPTED("ACCEPTED"),
    DECLINED("DECLINED"),
    CANCELED("CANCELED");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApplicationStatusDTO {
            return values().first { it -> it.value == value }
        }
    }
}
