package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

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
    val detail: TaskSubmissionReviewDetailDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
