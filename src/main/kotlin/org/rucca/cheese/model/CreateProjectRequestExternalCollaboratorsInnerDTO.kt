package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param userId */
data class CreateProjectRequestExternalCollaboratorsInnerDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("userId")
    val userId: kotlin.Long? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
