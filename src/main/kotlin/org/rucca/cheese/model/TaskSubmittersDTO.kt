package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param total
 * @param examples
 */
data class TaskSubmittersDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("total", required = true)
    val total: kotlin.Int,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("examples", required = true)
    val examples: kotlin.collections.List<TaskSubmittersExamplesInnerDTO>,
) {}
