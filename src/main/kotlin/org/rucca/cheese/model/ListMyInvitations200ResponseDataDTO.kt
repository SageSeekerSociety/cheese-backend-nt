package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param invitations
 * @param page
 */
data class ListMyInvitations200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("invitations", required = true)
    val invitations: kotlin.collections.List<TeamMembershipApplicationDTO>,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("page", required = true)
    val page: PageDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
