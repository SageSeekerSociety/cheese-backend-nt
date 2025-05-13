package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param conversation
 * @param quota
 */
data class TaskAIAdviceConversationResponseDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("conversation", required = true)
    val conversation: TaskAIAdviceConversationDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("quota", required = true)
    val quota: QuotaInfoDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
