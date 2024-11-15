package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

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
 * @param deadline
 * @param participantLimit
 * @param space
 * @param team
 * @param joinable Only has value when: 'queryJoinablity' == true
 * @param joinableAsTeam
 * @param submittable Only has value when: 'querySubmitability' == true
 * @param submittableAsTeam
 * @param rank
 * @param approved
 * @param rejectReason
 * @param joined
 * @param joinedAsTeam
 * @param joinedApproved
 * @param joinedApprovedAsTeam
 * @param joinedDisapproved
 * @param joinedDisapprovedAsTeam
 * @param joinedNotApprovedOrDisapproved
 * @param joinedNotApprovedOrDisapprovedAsTeam
 * @param topics
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
    @get:JsonProperty("team")
    val team: TeamDTO? = null,
    @Schema(example = "null", description = "Only has value when: 'queryJoinablity' == true")
    @get:JsonProperty("joinable")
    val joinable: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinableAsTeam")
    val joinableAsTeam: kotlin.collections.List<TeamSummaryDTO>? = null,
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
    @get:JsonProperty("joinedAsTeam")
    val joinedAsTeam: kotlin.collections.List<TeamSummaryDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedApproved")
    val joinedApproved: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedApprovedAsTeam")
    val joinedApprovedAsTeam: kotlin.collections.List<TeamSummaryDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedDisapproved")
    val joinedDisapproved: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedDisapprovedAsTeam")
    val joinedDisapprovedAsTeam: kotlin.collections.List<TeamSummaryDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedNotApprovedOrDisapproved")
    val joinedNotApprovedOrDisapproved: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("joinedNotApprovedOrDisapprovedAsTeam")
    val joinedNotApprovedOrDisapprovedAsTeam: kotlin.collections.List<TeamSummaryDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("topics")
    val topics: kotlin.collections.List<TopicDTO>? = null
) {}
