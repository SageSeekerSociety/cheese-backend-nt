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
 * @param classificationTopics
 */
data class PatchSpaceRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("intro")
    val intro: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("avatarId")
    val avatarId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("enableRank")
    val enableRank: kotlin.Boolean? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("announcements")
    val announcements: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("taskTemplates")
    val taskTemplates: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("classificationTopics")
    val classificationTopics: kotlin.collections.List<kotlin.Long>? = null
) {}
