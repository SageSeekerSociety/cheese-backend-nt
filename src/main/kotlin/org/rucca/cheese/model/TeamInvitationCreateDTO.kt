package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param userId The ID of the user to invite.
 * @param role
 * @param message Optional message accompanying the invitation.
 */
data class TeamInvitationCreateDTO(
    @Schema(example = "12345", required = true, description = "The ID of the user to invite.")
    @get:JsonProperty("userId", required = true)
    val userId: kotlin.Long,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("role")
    val role: TeamMemberRoleTypeDTO? = null,
    @Schema(
        example = "We'd love for you to join our project team!",
        description = "Optional message accompanying the invitation.",
    )
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
