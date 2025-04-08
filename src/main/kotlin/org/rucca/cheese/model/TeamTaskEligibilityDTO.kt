package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * Provides eligibility details for a specific team in the context of a task.
 *
 * @param team
 * @param eligibility
 */
data class TeamTaskEligibilityDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("team", required = true)
    val team: TeamSummaryDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("eligibility", required = true)
    val eligibility: EligibilityStatusDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
