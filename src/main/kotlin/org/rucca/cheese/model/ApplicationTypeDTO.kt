package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Type of the membership application. REQUEST = User asks to join. INVITATION = Team asks user to
 * join. Values: REQUEST,INVITATION
 */
enum class ApplicationTypeDTO(@get:JsonValue val value: kotlin.String) {

    REQUEST("REQUEST"),
    INVITATION("INVITATION");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApplicationTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
