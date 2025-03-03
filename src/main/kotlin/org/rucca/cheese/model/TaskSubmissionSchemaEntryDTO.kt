package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param prompt
 * @param type
 */
data class TaskSubmissionSchemaEntryDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("prompt", required = true)
    val prompt: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: TaskSubmissionTypeDTO,
) {}
