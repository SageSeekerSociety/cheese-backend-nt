package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param code
 * @param message
 */
data class KnowledgeDelete200ResponseDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("code", required = true)
    val code: kotlin.Int = 0,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("message", required = true)
    val message: kotlin.String,
) {}
