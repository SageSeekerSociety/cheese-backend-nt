package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param stage
 * @param description
 * @param resources
 */
data class TaskAIAdviceLearningPathsInnerDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("stage")
    val stage: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("resources")
    val resources: kotlin.collections.List<TaskAIAdviceLearningPathsInnerResourcesInnerDTO>? = null,
) {}
