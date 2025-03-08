package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Values: document,link,text,image */
enum class KnowledgeTypeDTO(@get:JsonValue val value: kotlin.String) {

    document("document"),
    link("link"),
    text("text"),
    image("image");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): KnowledgeTypeDTO {
            return values().first { it -> it.value == value }
        }
    }
}
