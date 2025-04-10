package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

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
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
