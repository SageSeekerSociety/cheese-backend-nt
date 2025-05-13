package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param user
 * @param role
 * @param createdAt
 * @param updatedAt
 * @param notes 项目成员备注
 */
data class ProjectMembershipDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("user", required = true)
    val user: UserDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: ProjectMemberRoleDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(example = "null", description = "项目成员备注")
    @get:JsonProperty("notes")
    val notes: kotlin.String? = "",
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
