package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** @param userId */
data class ProjectsPostRequestExternalCollaboratorsInnerDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("userId")
    val userId: kotlin.Long? = null
) {}
