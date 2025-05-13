package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param description
 * @param followupQuestions
 * @param stage
 * @param resources
 */
data class TaskAIAdviceLearningPathsInnerDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("followup_questions", required = true)
    val followupQuestions: kotlin.collections.List<kotlin.String>,
    @Schema(example = "null", description = "")
    @get:JsonProperty("stage")
    val stage: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("resources")
    val resources: kotlin.collections.List<TaskAIAdviceLearningPathsInnerResourcesInnerDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
