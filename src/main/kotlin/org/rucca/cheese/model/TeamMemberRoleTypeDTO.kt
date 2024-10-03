package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 小组角色 Values: OWNER,ADMIN,MEMBER */
enum class TeamMemberRoleTypeDTO(@get:JsonValue val value: kotlin.String) {

    OWNER("OWNER"),
    ADMIN("ADMIN"),
    MEMBER("MEMBER");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): TeamMemberRoleTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
