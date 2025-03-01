package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param name
 * @param description
 * @param type
 * @param content
 * @param projectIds
 * @param labels
 */
data class KnowledgePatchRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("type")
    val type: KnowledgePatchRequestDTO.Type? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("projectIds")
    val projectIds: kotlin.collections.List<kotlin.Long>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("labels")
    val labels: kotlin.collections.List<kotlin.String>? = null,
) {

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
}
