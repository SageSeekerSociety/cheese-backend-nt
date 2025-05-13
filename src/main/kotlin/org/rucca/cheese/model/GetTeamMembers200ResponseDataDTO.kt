package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param members
 * @param allMembersVerified
 */
data class GetTeamMembers200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("members", required = true)
    val members: kotlin.collections.List<TeamMemberDTO>,
    @Schema(example = "null", description = "")
    @get:JsonProperty("allMembersVerified")
    val allMembersVerified: kotlin.Boolean? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
