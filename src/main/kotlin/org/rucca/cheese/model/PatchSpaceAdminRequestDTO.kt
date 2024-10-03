package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/** @param role */
data class PatchSpaceAdminRequestDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("role")
    val role: SpaceAdminRoleTypeDTO? = null
) {}
