package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param name
 * @param submitterType
 * @param creator
 * @param deadline
 * @param resubmittable
 * @param editable
 * @param description
 * @param submissionSchema
 * @param submitters
 * @param updatedAt
 * @param createdAt
 * @param joinable Only has value when: 'queryJoinablity' == true
 * @param joinableAsTeam
 * @param submittable Only has value when: 'querySubmitability' == true
 * @param submittableAsTeam
 * @param rank
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
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("creator", required = true)
    val creator: UserDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("deadline", required = true)
    val deadline: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("resubmittable", required = true)
    val resubmittable: kotlin.Boolean,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("editable", required = true)
    val editable: kotlin.Boolean,
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
    val rank: kotlin.Int? = null
) {}
