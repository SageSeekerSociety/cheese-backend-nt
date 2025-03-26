package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 项目成员角色 Values: LEADER,MEMBER,EXTERNAL */
enum class ProjectMemberRoleDTO(@get:JsonValue val value: kotlin.String) {

    LEADER("LEADER"),
    MEMBER("MEMBER"),
    EXTERNAL("EXTERNAL");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ProjectMemberRoleDTO {
            return values().first { it -> it.value == value }
        }
    }
}
