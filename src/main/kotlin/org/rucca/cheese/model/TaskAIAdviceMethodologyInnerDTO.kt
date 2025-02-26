package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param step
 * @param description
 * @param followupQuestions
 * @param estimatedTime
 */
data class TaskAIAdviceMethodologyInnerDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("step", required = true)
    val step: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("followup_questions", required = true)
    val followupQuestions: kotlin.collections.List<kotlin.String>,
    @Schema(example = "null", description = "")
    @get:JsonProperty("estimated_time")
    val estimatedTime: kotlin.String? = null,
) {}
