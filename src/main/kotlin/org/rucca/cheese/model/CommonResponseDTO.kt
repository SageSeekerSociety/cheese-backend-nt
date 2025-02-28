package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param code
 * @param message
 */
data class CommonResponseDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("code")
    val code: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
) {}
