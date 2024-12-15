package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param intro
 * @param description
 * @param name
 * @param avatarId
 * @param announcements
 * @param taskTemplates
 * @param enableRank
 * @param classificationTopics
 */
data class PostProjectRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("code")
    val code: kotlin.Int? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("message")
    val message: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("project")
    val projects: kotlin.collections.List<ProjectDTO>? = null,

) {}
