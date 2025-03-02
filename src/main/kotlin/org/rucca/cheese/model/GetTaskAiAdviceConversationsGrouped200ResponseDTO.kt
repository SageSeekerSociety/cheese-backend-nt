package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param code
 * @param &#x60;data&#x60;
 * @param message
 */
data class GetTaskAiAdviceConversationsGrouped200ResponseDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("code")
    val code: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("data")
    val `data`: GetTaskAiAdviceConversationsGrouped200ResponseDataDTO? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
