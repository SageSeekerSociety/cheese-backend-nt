package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param name
 * @param avatarId
 * @param intro
 * @param isLeader
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
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("realNameInfo")
    val realNameInfo: TaskParticipantRealNameInfoDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
