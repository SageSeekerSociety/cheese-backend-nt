package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param id
 * @param name
 */
data class TopicDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String
) {}
