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
data class GetTaskSubmissions200ResponseDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("code", required = true)
    val code: kotlin.Int,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true)
    val `data`: GetTaskSubmissions200ResponseDataDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("message", required = true)
    val message: kotlin.String,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
