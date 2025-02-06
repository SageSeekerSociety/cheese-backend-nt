package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Has value only when 'reviewed' is true
 *
 * @param accepted
 * @param score
 * @param comment
 */
data class TaskSubmissionReviewDetailDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("accepted", required = true)
    val accepted: kotlin.Boolean,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("score", required = true)
    val score: kotlin.Int,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("comment", required = true)
    val comment: kotlin.String,
) {}
