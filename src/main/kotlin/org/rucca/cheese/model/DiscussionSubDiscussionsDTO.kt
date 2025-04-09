package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param count
 * @param examples
 */
data class DiscussionSubDiscussionsDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("count")
    val count: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("examples")
    val examples: kotlin.collections.List<DiscussionDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
