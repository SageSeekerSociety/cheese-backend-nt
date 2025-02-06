package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param submission
 * @param hasUpgradedParticipantRank
 */
data class PostTaskSubmissionReview200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submission", required = true)
    val submission: TaskSubmissionDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("hasUpgradedParticipantRank", required = true)
    val hasUpgradedParticipantRank: kotlin.Boolean,
) {}
