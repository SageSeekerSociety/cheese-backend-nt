package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid
import javax.validation.constraints.Pattern

/**
 * @param name
 * @param description
 * @param colorCode
 * @param startDate 项目开始时间戳(毫秒)
 * @param endDate 项目结束时间戳(毫秒)
 * @param leaderId
 * @param content
 * @param parentId 父项目ID
 * @param externalTaskId
 * @param githubRepo
 * @param externalCollaborators
 */
data class ProjectsPostRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @get:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("colorCode", required = true)
    val colorCode: kotlin.String,
    @Schema(example = "null", required = true, description = "项目开始时间戳(毫秒)")
    @get:JsonProperty("startDate", required = true)
    val startDate: kotlin.Long,
    @Schema(example = "null", required = true, description = "项目结束时间戳(毫秒)")
    @get:JsonProperty("endDate", required = true)
    val endDate: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("leaderId", required = true)
    val leaderId: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: ProjectContentDTO,
    @Schema(example = "null", description = "父项目ID")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("externalTaskId")
    val externalTaskId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("githubRepo")
    val githubRepo: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("externalCollaborators")
    val externalCollaborators:
        kotlin.collections.List<ProjectsPostRequestExternalCollaboratorsInnerDTO>? =
        null,
) {}
