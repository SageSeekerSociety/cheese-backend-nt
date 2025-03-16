package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param emoji */
data class ReactToDiscussionRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("emoji", required = true)
    val emoji: kotlin.String
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
