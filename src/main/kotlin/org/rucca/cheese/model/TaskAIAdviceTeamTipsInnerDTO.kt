package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param role
 * @param description
 * @param collaborationTips
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
) {}
