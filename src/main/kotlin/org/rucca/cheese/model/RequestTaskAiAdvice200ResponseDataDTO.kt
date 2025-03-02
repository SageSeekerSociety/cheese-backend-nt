package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param status
 * @param quota
 */
data class RequestTaskAiAdvice200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("status")
    val status: TaskAIAdviceGenerationStatusDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("quota")
    val quota: QuotaInfoDTO? = null,
) {}
