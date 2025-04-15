package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * Provides eligibility details for a user and their teams in the context of a task.
 *
 * @param user
 * @param teams Eligibility status of the user's teams for the task.
 */
data class ParticipationEligibilityDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("user")
    val user: EligibilityStatusDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "Eligibility status of the user's teams for the task.")
    @get:JsonProperty("teams")
    val teams: kotlin.collections.List<TeamTaskEligibilityDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
