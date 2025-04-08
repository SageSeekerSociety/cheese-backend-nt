package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param name
 * @param submitterType
 * @param resubmittable
 * @param editable
 * @param intro
 * @param description
 * @param submissionSchema
 * @param space
 * @param deadline
 * @param defaultDeadline
 * @param categoryId The ID of the category to assign this task to. Requires spaceId to be set.
 * @param rank
 * @param topics
 * @param requireRealName Whether the task requires real name information
 * @param minTeamSize Minimum team size required to submit the task, only valid if submitterType is
 *   TEAM. `undefined` if not specified.
 * @param maxTeamSize Maximum team size allowed to submit the task, only valid if submitterType is
 *   TEAM. `undefined` if not specified.
 * @param participantLimit Maximum number of participants allowed in the task. `undefined` if not
 *   specified.
 */
data class PostTaskRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submitterType", required = true)
    val submitterType: TaskSubmitterTypeDTO,
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
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("space", required = true)
    val space: kotlin.Long,
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("defaultDeadline")
    val defaultDeadline: kotlin.Long? = 30L,
    @Schema(
        example = "123",
        description = "The ID of the category to assign this task to. Requires spaceId to be set.",
    )
    @get:JsonProperty("categoryId")
    val categoryId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("rank")
    val rank: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("topics")
    val topics: kotlin.collections.List<kotlin.Long>? = arrayListOf(),
    @Schema(example = "null", description = "Whether the task requires real name information")
    @get:JsonProperty("requireRealName")
    val requireRealName: kotlin.Boolean? = false,
    @Schema(
        example = "null",
        description =
            "Minimum team size required to submit the task, only valid if submitterType is TEAM. `undefined` if not specified.",
    )
    @get:JsonProperty("minTeamSize")
    val minTeamSize: kotlin.Long? = null,
    @Schema(
        example = "null",
        description =
            "Maximum team size allowed to submit the task, only valid if submitterType is TEAM. `undefined` if not specified.",
    )
    @get:JsonProperty("maxTeamSize")
    val maxTeamSize: kotlin.Long? = null,
    @Schema(
        example = "null",
        description =
            "Maximum number of participants allowed in the task. `undefined` if not specified.",
    )
    @get:JsonProperty("participantLimit")
    val participantLimit: kotlin.Int? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
