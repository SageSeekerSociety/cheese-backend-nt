package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param code
 * @param message
 */
data class GetTaskAiAdvice400ResponseDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("code")
    val code: kotlin.Int? = null,
    @Schema(
        example = "Task AI advice is not ready yet. Current status: PROCESSING",
        description = "",
    )
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
