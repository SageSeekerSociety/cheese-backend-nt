package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param hasParticipation
 * @param identities
 */
data class TaskParticipationInfoDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("hasParticipation", required = true)
    val hasParticipation: kotlin.Boolean,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("identities", required = true)
    val identities: kotlin.collections.List<TaskParticipationIdentityDTO>,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
