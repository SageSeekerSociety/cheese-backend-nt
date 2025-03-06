package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param title
 * @param keyPoints
 */
data class TaskAIAdviceTopicSummaryDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("title")
    val title: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("key_points")
    val keyPoints: kotlin.collections.List<kotlin.String>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
