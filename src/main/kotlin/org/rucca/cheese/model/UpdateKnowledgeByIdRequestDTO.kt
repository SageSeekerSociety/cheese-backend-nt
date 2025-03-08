package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param name
 * @param description
 * @param type
 * @param content
 * @param projectIds
 * @param labels
 */
data class UpdateKnowledgeByIdRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("type")
    val type: UpdateKnowledgeByIdRequestDTO.Type? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("projectIds")
    val projectIds: kotlin.collections.List<kotlin.Long>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("labels")
    val labels: kotlin.collections.List<kotlin.String>? = null,
) : Serializable {

    /** Values: document,link,text,image */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        document("document"),
        link("link"),
        text("text"),
        image("image");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().first { it -> it.value == value }
            }
        }
    }

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
