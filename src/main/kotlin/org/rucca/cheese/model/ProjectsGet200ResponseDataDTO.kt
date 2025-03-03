package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param projects
 * @param page
 */
data class ProjectsGet200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("projects")
    val projects: kotlin.collections.List<ProjectDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("page")
    val page: PageDTO? = null,
) {}
