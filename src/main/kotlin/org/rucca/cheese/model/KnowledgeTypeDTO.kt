package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: DOCUMENT,LINK,TEXT,IMAGE */
enum class KnowledgeTypeDTO(@get:JsonValue val value: kotlin.String) {

    DOCUMENT("DOCUMENT"),
    LINK("LINK"),
    TEXT("TEXT"),
    IMAGE("IMAGE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): KnowledgeTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
