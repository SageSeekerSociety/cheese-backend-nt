package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: OWNER,ADMIN */
enum class SpaceAdminRoleTypeDTO(@get:JsonValue val value: kotlin.String) {

    OWNER("OWNER"),
    ADMIN("ADMIN");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): SpaceAdminRoleTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
