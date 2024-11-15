package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/** @param participants */
data class PostTaskParticipant200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("participants")
    val participants: kotlin.collections.List<TaskMembershipDTO>? = null
) {}
