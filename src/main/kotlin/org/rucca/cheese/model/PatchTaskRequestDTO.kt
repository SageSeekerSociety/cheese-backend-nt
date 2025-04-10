package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

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
 * @param requireRealName
 * @param categoryId ID of the category to which the task belongs.
 * @param minTeamSize Minimum team size required for the task.
 * @param maxTeamSize Maximum team size allowed for the task.
 * @param teamLockingPolicy
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
    @Schema(example = "null", description = "")
    @get:JsonProperty("requireRealName")
    val requireRealName: kotlin.Boolean? = null,
    @Schema(example = "1", description = "ID of the category to which the task belongs.")
    @get:JsonProperty("categoryId")
    val categoryId: kotlin.Long? = null,
    @Schema(example = "1", description = "Minimum team size required for the task.")
    @get:JsonProperty("minTeamSize")
    val minTeamSize: kotlin.Int? = null,
    @Schema(example = "10", description = "Maximum team size allowed for the task.")
    @get:JsonProperty("maxTeamSize")
    val maxTeamSize: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("teamLockingPolicy")
    val teamLockingPolicy: TeamMembershipLockPolicyDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
