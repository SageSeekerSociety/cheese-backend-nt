package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/** @param role */
data class PatchTeamMemberRequestDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("role")
    val role: TeamMemberRoleTypeDTO? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
