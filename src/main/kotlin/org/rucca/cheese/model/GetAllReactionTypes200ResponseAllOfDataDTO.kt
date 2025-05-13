package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/** @param reactionTypes */
data class GetAllReactionTypes200ResponseAllOfDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("reactionTypes")
    val reactionTypes: kotlin.collections.List<ReactionTypeDTO>? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
