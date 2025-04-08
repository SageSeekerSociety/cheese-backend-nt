package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * Represents the eligibility status (e.g., for joining or submitting) for a user or a team
 * regarding a specific task.
 *
 * @param eligible Indicates whether the user/team is eligible.
 * @param reasons A list of reasons why the user/team is *not* eligible. Empty if eligible is true.
 */
data class EligibilityStatusDTO(
    @Schema(
        example = "null",
        required = true,
        description = "Indicates whether the user/team is eligible.",
    )
    @get:JsonProperty("eligible", required = true)
    val eligible: kotlin.Boolean,
    @field:Valid
    @Schema(
        example = "null",
        description =
            "A list of reasons why the user/team is *not* eligible. Empty if eligible is true.",
    )
    @get:JsonProperty("reasons")
    val reasons: kotlin.collections.List<EligibilityRejectReasonInfoDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
