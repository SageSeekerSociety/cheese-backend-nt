package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param code
 * @param message
 * @param &#x60;data&#x60;
 */
data class KnowledgePost200ResponseDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("code", required = true)
    val code: kotlin.Int = 0,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true)
    val `data`: KnowledgePost200ResponseDataDTO,   
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("message", required = true)
    val message: kotlin.String = "success",
) {}
