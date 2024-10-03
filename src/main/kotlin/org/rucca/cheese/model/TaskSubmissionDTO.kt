package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param member
 * @param submitter
 * @param version
 * @param createdAt
 * @param updatedAt
 * @param content
 * @param review
 */
data class TaskSubmissionDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("member", required = true)
    val member: TaskParticipantSummaryDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submitter", required = true)
    val submitter: UserDTO,
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
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.collections.List<TaskSubmissionContentEntryDTO>,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("review")
    val review: TaskSubmissionReviewDTO? = null
) {}
