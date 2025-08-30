package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** 可被讨论的模型类型 Values: PROJECT */
enum class DiscussableModelTypeDTO(@get:JsonValue val value: kotlin.String) {

    PROJECT("PROJECT");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): DiscussableModelTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
