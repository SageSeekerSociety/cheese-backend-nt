package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id
 * @param name
 * @param intro
 * @param avatarId
 * @param allMembersVerified
 * @param memberRealNameStatus
 * @param updatedAt
 * @param createdAt
 */
data class TeamSummaryDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("intro", required = true)
    val intro: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("avatarId", required = true)
    val avatarId: kotlin.Long,
    @Schema(example = "null", description = "")
    @get:JsonProperty("allMembersVerified")
    val allMembersVerified: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("memberRealNameStatus")
    val memberRealNameStatus: kotlin.collections.List<TeamMemberRealNameStatusDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("updatedAt")
    val updatedAt: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("createdAt")
    val createdAt: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
