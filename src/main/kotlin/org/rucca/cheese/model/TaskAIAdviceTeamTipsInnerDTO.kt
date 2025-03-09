package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param role
 * @param description
 * @param collaborationTips
 * @param followupQuestions
 */
data class TaskAIAdviceTeamTipsInnerDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("collaboration_tips", required = true)
    val collaborationTips: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("followup_questions", required = true)
    val followupQuestions: kotlin.collections.List<kotlin.String>,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
