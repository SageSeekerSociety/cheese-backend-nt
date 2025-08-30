package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: VIEW,EXPORT */
enum class UserIdentityAccessTypeDTO(@get:JsonValue val value: kotlin.String) {

    VIEW("VIEW"),
    EXPORT("EXPORT");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): UserIdentityAccessTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
