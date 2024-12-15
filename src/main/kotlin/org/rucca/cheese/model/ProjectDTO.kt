package org.rucca.cheese.model

import Project
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.User
import javax.validation.Valid

/**
 * @param id
 * @param description
 * @param name
 * @param updatedAt
 * @param createdAt
 */
data class ProjectDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("color_code", required = true)
    val colorCode: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("startDate", required = true)
    val startDate: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("endDate", required = true)
    val endDate: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("parent", required = true)
    val parent: Project,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("leader", required = true)
    val leader: User,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("external_task_id", required = true)
    val externalTaskId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("github_repo", required = true)
    val githubRepo: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("deletedAt", required = true)
    val deletedAt: kotlin.Long,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("team", required = true)
    val team: Team,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("version", required = true)
    val version: kotlin.Int

    ) {}
