package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param name
 * @param type
 * @param url
 */
data class TaskAIAdviceLearningPathsInnerResourcesInnerDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: kotlin.String,
    @Schema(example = "null", description = "")
    @get:JsonProperty("url")
    val url: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
