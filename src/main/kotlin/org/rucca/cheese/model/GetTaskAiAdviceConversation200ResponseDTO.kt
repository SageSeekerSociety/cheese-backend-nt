package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param code
 * @param &#x60;data&#x60;
 * @param message
 */
data class GetTaskAiAdviceConversation200ResponseDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("code")
    val code: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("data")
    val `data`: GetTaskAiAdviceConversation200ResponseDataDTO? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
) {}
