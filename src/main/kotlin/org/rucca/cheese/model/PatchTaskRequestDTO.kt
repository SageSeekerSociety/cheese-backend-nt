package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param name
 * @param hasDeadline
 * @param deadline
 * @param hasParticipantLimit
 * @param participantLimit
 * @param defaultDeadline
 * @param resubmittable
 * @param editable
 * @param intro
 * @param description
 * @param submissionSchema
 * @param hasRank
 * @param rank
 * @param approved
 * @param rejectReason
 * @param topics
 */
data class PatchTaskRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("hasDeadline")
    val hasDeadline: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("hasParticipantLimit")
    val hasParticipantLimit: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("participantLimit")
    val participantLimit: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("defaultDeadline")
    val defaultDeadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("resubmittable")
    val resubmittable: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("editable")
    val editable: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("intro")
    val intro: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("submissionSchema")
    val submissionSchema: kotlin.collections.List<TaskSubmissionSchemaEntryDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("hasRank")
    val hasRank: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("rank")
    val rank: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("approved")
    val approved: ApproveTypeDTO? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("rejectReason")
    val rejectReason: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("topics")
    val topics: kotlin.collections.List<kotlin.Long>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
