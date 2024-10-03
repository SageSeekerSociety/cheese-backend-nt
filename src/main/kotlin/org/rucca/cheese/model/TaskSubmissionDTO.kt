package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param version
 * @param createdAt
 * @param updatedAt
 * @param member
 * @param submitter
 * @param content
 */
data class TaskSubmissionDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("version", required = true)
    val version: kotlin.Int,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("member")
    val member: TaskParticipantSummaryDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("submitter")
    val submitter: UserDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.collections.List<TaskSubmissionContentEntryDTO>? = null
) {}
