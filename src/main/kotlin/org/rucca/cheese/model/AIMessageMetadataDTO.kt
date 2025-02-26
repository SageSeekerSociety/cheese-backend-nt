package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

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
) {}
