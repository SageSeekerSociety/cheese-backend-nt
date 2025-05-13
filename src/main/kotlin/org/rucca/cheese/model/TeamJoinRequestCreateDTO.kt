package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param message Optional message accompanying the join request. */
data class TeamJoinRequestCreateDTO(
    @Schema(
        example = "Hi, I'd like to join your team because...",
        description = "Optional message accompanying the join request.",
    )
    @get:JsonProperty("message")
    val message: kotlin.String? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
