package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param name
 * @param deadline
 * @param resubmittable
 * @param editable
 * @param description
 * @param submissionSchema
 */
data class PatchTaskRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("resubmittable")
    val resubmittable: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("editable")
    val editable: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("submissionSchema")
    val submissionSchema: kotlin.collections.List<TaskSubmissionSchemaEntryDTO>? = null
) {}