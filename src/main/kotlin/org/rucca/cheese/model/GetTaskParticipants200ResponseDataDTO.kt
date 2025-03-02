package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/** @param participants */
data class GetTaskParticipants200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("participants", required = true)
    val participants: kotlin.collections.List<TaskMembershipDTO>
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
