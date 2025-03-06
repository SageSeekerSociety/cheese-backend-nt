package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** @param emoji */
data class DiscussionsDiscussionIdReactionsPostRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("emoji", required = true)
    val emoji: kotlin.String
) {}
