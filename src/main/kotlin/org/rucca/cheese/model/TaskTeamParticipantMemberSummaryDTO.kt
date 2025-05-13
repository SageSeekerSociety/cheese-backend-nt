package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param name
 * @param avatarId
 * @param intro
 * @param isLeader
 * @param userId
 * @param participantMemberId
 * @param realNameInfo
 */
data class TaskTeamParticipantMemberSummaryDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("avatarId", required = true)
    val avatarId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("intro", required = true)
    val intro: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isLeader", required = true)
    val isLeader: kotlin.Boolean,
    @Schema(example = "null", description = "")
    @get:JsonProperty("userId")
    val userId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("participantMemberId")
    val participantMemberId: java.util.UUID? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("realNameInfo")
    val realNameInfo: TaskParticipantRealNameInfoDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
