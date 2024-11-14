package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param intro
 * @param name
 * @param avatarId
 * @param approved
 * @param realNameInfo
 */
data class TaskParticipantSummaryDTO(
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
    @get:JsonProperty("approved", required = true)
    val approved: ApproveTypeDTO,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("realNameInfo")
    val realNameInfo: TaskParticipantRealNameInfoDTO? = null
) {}
