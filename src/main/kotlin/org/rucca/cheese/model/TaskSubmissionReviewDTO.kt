package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param reviewed
 * @param detail
 */
data class TaskSubmissionReviewDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reviewed", required = true)
    val reviewed: kotlin.Boolean,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("detail")
    val detail: TaskSubmissionReviewDetailDTO? = null
) {}
