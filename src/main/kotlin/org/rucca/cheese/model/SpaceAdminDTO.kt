package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param role
 * @param user
 * @param updatedAt
 * @param createdAt
 */
data class SpaceAdminDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: SpaceAdminRoleTypeDTO,
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
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
