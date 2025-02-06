package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param role
 * @param userId
 */
data class PostTeamMemberRequestDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: TeamMemberRoleTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("user_id", required = true)
    val userId: kotlin.Long,
) {}
