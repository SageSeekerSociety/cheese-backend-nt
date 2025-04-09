package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param reactionType
 * @param count
 * @param hasReacted
 */
data class DiscussionReactionSummaryDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reactionType", required = true)
    val reactionType: ReactionTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("count", required = true)
    val count: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("hasReacted", required = true)
    val hasReacted: kotlin.Boolean,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
