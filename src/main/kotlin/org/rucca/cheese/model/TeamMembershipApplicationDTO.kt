package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id Unique identifier for the application/invitation.
 * @param user
 * @param team
 * @param initiator
 * @param type
 * @param status
 * @param role
 * @param message Message associated with the application.
 * @param createdAt Timestamp when the application was created.
 * @param updatedAt Timestamp when the application was last updated.
 * @param processedBy
 * @param processedAt Timestamp when the application was processed.
 */
data class TeamMembershipApplicationDTO(
    @Schema(
        example = "null",
        required = true,
        readOnly = true,
        description = "Unique identifier for the application/invitation.",
    )
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("user", required = true)
    val user: UserDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("team", required = true)
    val team: TeamSummaryDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("initiator", required = true)
    val initiator: UserDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: ApplicationTypeDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true)
    val status: ApplicationStatusDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: TeamMemberRoleTypeDTO,
    @Schema(
        example = "null",
        required = true,
        description = "Message associated with the application.",
    )
    @get:JsonProperty("message", required = true)
    val message: kotlin.String?,
    @Schema(
        example = "null",
        required = true,
        readOnly = true,
        description = "Timestamp when the application was created.",
    )
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(
        example = "null",
        required = true,
        readOnly = true,
        description = "Timestamp when the application was last updated.",
    )
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("processedBy")
    val processedBy: UserDTO? = null,
    @Schema(
        example = "null",
        readOnly = true,
        description = "Timestamp when the application was processed.",
    )
    @get:JsonProperty("processedAt")
    val processedAt: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
