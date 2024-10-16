package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param intro
 * @param description
 * @param name
 * @param avatarId
 * @param enableRank
 * @param announcements
 * @param taskTemplates
 */
data class PostSpaceRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("intro", required = true)
    val intro: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("avatarId", required = true)
    val avatarId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("enableRank", required = true)
    val enableRank: kotlin.Boolean = false,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("announcements", required = true)
    val announcements: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("taskTemplates", required = true)
    val taskTemplates: kotlin.String
) {}
