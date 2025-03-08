package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param accepted
 * @param score
 * @param comment
 */
data class PatchTaskSubmissionReviewRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("accepted")
    val accepted: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("score")
    val score: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("comment")
    val comment: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
