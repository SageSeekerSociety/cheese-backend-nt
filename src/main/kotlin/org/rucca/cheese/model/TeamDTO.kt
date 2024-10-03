package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param intro
 * @param name
 * @param avatarId
 * @param owner
 * @param admins
 * @param updatedAt
 * @param createdAt
 * @param members
 * @param joined
 * @param role
 */
data class TeamDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("intro", required = true)
    val intro: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("avatarId", required = true)
    val avatarId: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("owner", required = true)
    val owner: UserDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("admins", required = true)
    val admins: TeamAdminsDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("members", required = true)
    val members: TeamMembersDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("joined", required = true)
    val joined: kotlin.Boolean,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("role")
    val role: TeamMemberRoleTypeDTO? = null
) {}
