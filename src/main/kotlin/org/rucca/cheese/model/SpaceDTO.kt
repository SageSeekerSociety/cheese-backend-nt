package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param intro
 * @param description
 * @param name
 * @param avatarId
 * @param admins
 * @param updatedAt
 * @param createdAt
 * @param enableRank
 * @param myRank Only has value when: 'queryJoinablity' == true && 'enableRank' == true
 */
data class SpaceDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
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
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("admins", required = true)
    val admins: kotlin.collections.List<SpaceAdminDTO>,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("enableRank", required = true)
    val enableRank: kotlin.Boolean,
    @Schema(
        example = "null",
        description = "Only has value when: 'queryJoinablity' == true && 'enableRank' == true"
    )
    @get:JsonProperty("myRank")
    val myRank: kotlin.Int? = null
) {}
