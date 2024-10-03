package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param intro
 * @param name
 * @param avatarId
 */
data class PatchSpaceRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("intro")
    val intro: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("avatarId")
    val avatarId: kotlin.Long? = null
) {}
