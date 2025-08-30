package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: TASK */
enum class UserIdentityAccessModuleTypeDTO(@get:JsonValue val value: kotlin.String) {

    TASK("TASK");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): UserIdentityAccessModuleTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
