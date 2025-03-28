package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: MATERIAL,LINK,TEXT,CODE */
enum class KnowledgeTypeDTO(@get:JsonValue val value: kotlin.String) {

    MATERIAL("MATERIAL"),
    LINK("LINK"),
    TEXT("TEXT"),
    CODE("CODE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): KnowledgeTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
