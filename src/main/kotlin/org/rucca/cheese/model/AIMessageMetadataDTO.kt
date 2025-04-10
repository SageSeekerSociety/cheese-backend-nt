package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * AI消息元数据
 *
 * @param followupQuestions
 * @param references
 */
data class AIMessageMetadataDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("followupQuestions")
    val followupQuestions: kotlin.collections.List<kotlin.String>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("references")
    val references: kotlin.collections.List<ConversationReferenceDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
