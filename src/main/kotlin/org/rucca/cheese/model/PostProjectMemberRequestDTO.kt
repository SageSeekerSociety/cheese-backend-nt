package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param userId
 * @param role
 * @param notes
 */
data class PostProjectMemberRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("userId", required = true)
    val userId: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("role", required = true)
    val role: ProjectMemberRoleDTO,
    @Schema(example = "", description = "")
    @get:JsonProperty("notes")
    val notes: kotlin.String? = "",
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
