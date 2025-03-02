package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/** @param status */
data class GetTaskAiAdviceStatus200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("status")
    val status: TaskAIAdviceGenerationStatusDTO? = null
) {}
