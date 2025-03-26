package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param content */
data class PatchDiscussionRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.String? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
