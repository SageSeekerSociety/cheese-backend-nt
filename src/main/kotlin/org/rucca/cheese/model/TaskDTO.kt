package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param id
 * @param name
 * @param submitterType
 * @param defaultDeadline
 * @param creator
 * @param resubmittable
 * @param editable
 * @param intro
 * @param description
 * @param submissionSchema
 * @param submitters
 * @param updatedAt
 * @param createdAt
 * @param requireRealName Whether the task requires real name information
 * @param deadline
 * @param participantLimit
 * @param space
 * @param category
 * @param team
 * @param participationEligibility
 * @param submittable Only has value when: 'querySubmitability' == true
 * @param submittableAsTeam
 * @param rank
 * @param approved
 * @param rejectReason
 * @param joined
 * @param joinedTeams
 * @param userDeadline
 * @param topics
 * @param minTeamSize Minimum size of team that can join this task
 * @param maxTeamSize Maximum size of team that can join this task
 * @param teamLockingPolicy
 */
data class TaskDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submitterType", required = true)
    val submitterType: TaskSubmitterTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("defaultDeadline", required = true)
    val defaultDeadline: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("creator", required = true)
    val creator: UserDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("resubmittable", required = true)
    val resubmittable: kotlin.Boolean,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("editable", required = true)
    val editable: kotlin.Boolean,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("intro", required = true)
    val intro: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submissionSchema", required = true)
    val submissionSchema: kotlin.collections.List<TaskSubmissionSchemaEntryDTO>,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submitters", required = true)
    val submitters: TaskSubmittersDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(
        example = "null",
        required = true,
        description = "Whether the task requires real name information",
    )
    @get:JsonProperty("requireRealName", required = true)
    val requireRealName: kotlin.Boolean,
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("participantLimit")
    val participantLimit: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("space")
    val space: SpaceDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("category")
    val category: SpaceCategoryDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("team")
    val team: TeamDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("participationEligibility")
    val participationEligibility: ParticipationEligibilityDTO? = null,
    @Schema(example = "null", description = "Only has value when: 'querySubmitability' == true")
    @get:JsonProperty("submittable")
    val submittable: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("submittableAsTeam")
    val submittableAsTeam: kotlin.collections.List<TeamSummaryDTO>? = null,
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
    @get:JsonProperty("joined")
    val joined: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedTeams")
    val joinedTeams: kotlin.collections.List<TeamSummaryDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("userDeadline")
    val userDeadline: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("topics")
    val topics: kotlin.collections.List<TopicDTO>? = null,
    @Schema(example = "null", description = "Minimum size of team that can join this task")
    @get:JsonProperty("minTeamSize")
    val minTeamSize: kotlin.Int? = null,
    @Schema(example = "null", description = "Maximum size of team that can join this task")
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
