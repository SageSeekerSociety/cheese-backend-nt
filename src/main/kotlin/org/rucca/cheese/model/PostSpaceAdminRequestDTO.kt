package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param role
 * @param userId
 */
data class PostSpaceAdminRequestDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: SpaceAdminRoleTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("userId", required = true)
    val userId: kotlin.Long,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
