package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param code
 * @param message
 * @param &#x60;data&#x60;
 */
data class SetCollectiveNotificationStatus200ResponseDTO(
    @Schema(example = "200", required = true, description = "")
    @get:JsonProperty("code", required = true)
    val code: kotlin.Int,
    @Schema(example = "Success", required = true, description = "")
    @get:JsonProperty("message", required = true)
    val message: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true)
    val `data`: SetCollectiveNotificationStatus200ResponseDataDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
