package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param name
 * @param submitterType
 * @param defaultDeadline
 * @param resubmittable
 * @param editable
 * @param intro
 * @param description
 * @param submissionSchema
 * @param deadline
 * @param participantLimit
 * @param team
 * @param space
 * @param rank
 * @param topics
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
    @get:JsonProperty("defaultDeadline", required = true)
    val defaultDeadline: kotlin.Long = 30L,
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
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("participantLimit")
    val participantLimit: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("team")
    val team: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("space")
    val space: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("rank")
    val rank: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("topics")
    val topics: kotlin.collections.List<kotlin.Long>? = arrayListOf()
) {}
