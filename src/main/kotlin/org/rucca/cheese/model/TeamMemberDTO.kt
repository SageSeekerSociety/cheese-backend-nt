package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param role
 * @param user
 * @param updatedAt
 * @param createdAt
 * @param hasRealNameInfo
 */
data class TeamMemberDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: TeamMemberRoleTypeDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("user", required = true)
    val user: UserDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(example = "null", description = "")
    @get:JsonProperty("hasRealNameInfo")
    val hasRealNameInfo: kotlin.Boolean? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
