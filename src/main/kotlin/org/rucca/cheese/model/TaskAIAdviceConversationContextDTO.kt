package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param section
 * @param index 章节中的具体条目索引(从0开始)
 */
data class TaskAIAdviceConversationContextDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("section")
    val section: TaskAIAdviceConversationContextDTO.Section? = null,
    @Schema(example = "null", description = "章节中的具体条目索引(从0开始)")
    @get:JsonProperty("index")
    val index: kotlin.Int? = null,
) : Serializable {

    /** Values: knowledge_fields,learning_paths,methodology,team_tips */
    enum class Section(@get:JsonValue val value: kotlin.String) {

        knowledge_fields("knowledge_fields"),
        learning_paths("learning_paths"),
        methodology("methodology"),
        team_tips("team_tips");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Section {
                return values().first { it -> it.value == value }
            }
        }
    }

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
