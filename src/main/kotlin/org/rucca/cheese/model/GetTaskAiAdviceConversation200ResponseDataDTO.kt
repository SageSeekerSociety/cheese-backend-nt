package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/** @param conversations */
data class GetTaskAiAdviceConversation200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("conversations")
    val conversations: kotlin.collections.List<TaskAIAdviceConversationDTO>? = null
) {}
