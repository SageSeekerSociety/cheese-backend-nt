package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param name
 * @param type
 * @param content
 * @param description
 * @param projectIds
 * @param labels
 */
data class KnowledgePostRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: KnowledgePostRequestDTO.Type,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
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
