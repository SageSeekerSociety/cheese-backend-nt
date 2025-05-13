package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * Describes a specific reason for ineligibility.
 *
 * @param code
 * @param message User-friendly explanation of the reason.
 * @param details Optional additional details specific to the reason code (e.g., list of user IDs
 *   missing real name info).
 */
data class EligibilityRejectReasonInfoDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("code", required = true)
    val code: EligibilityRejectReasonCodeDTO,
    @Schema(
        example = "Team size must be at least 3 members.",
        required = true,
        description = "User-friendly explanation of the reason.",
    )
    @get:JsonProperty("message", required = true)
    val message: kotlin.String,
    @field:Valid
    @Schema(
        example = "{\"missingRealNameUserIds\":[101,105]}",
        description =
            "Optional additional details specific to the reason code (e.g., list of user IDs missing real name info).",
    )
    @get:JsonProperty("details")
    val details: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
