package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param code Response code
 * @param &#x60;data&#x60;
 * @param message Response message
 */
data class GetTaskTeams200ResponseDTO(
    @Schema(example = "200", description = "Response code")
    @get:JsonProperty("code")
    val code: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("data")
    val `data`: GetTaskTeams200ResponseDataDTO? = null,
    @Schema(example = "OK", description = "Response message")
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
