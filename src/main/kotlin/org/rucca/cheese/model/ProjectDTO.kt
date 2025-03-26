package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid
import javax.validation.constraints.Pattern

/**
 * @param id
 * @param name
 * @param description
 * @param startDate 项目开始时间戳(毫秒)
 * @param endDate 项目结束时间戳(毫秒)
 * @param team
 * @param leader
 * @param members
 * @param colorCode
 * @param parentId 父项目ID
 * @param externalTaskId
 * @param githubRepo
 * @param content
 * @param createdAt
 * @param updatedAt
 * @param archived
 * @param children
 */
data class ProjectDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "项目开始时间戳(毫秒)")
    @get:JsonProperty("startDate", required = true)
    val startDate: kotlin.Long,
    @Schema(example = "null", required = true, description = "项目结束时间戳(毫秒)")
    @get:JsonProperty("endDate", required = true)
    val endDate: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("team", required = true)
    val team: TeamDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("leader", required = true)
    val leader: UserDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("members", required = true)
    val members: ProjectMembersDTO,
    @get:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Schema(example = "null", description = "")
    @get:JsonProperty("colorCode")
    val colorCode: kotlin.String? = null,
    @Schema(example = "null", description = "父项目ID")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("externalTaskId")
    val externalTaskId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("githubRepo")
    val githubRepo: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("createdAt")
    val createdAt: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("updatedAt")
    val updatedAt: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("archived")
    val archived: kotlin.Boolean? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("children")
    val children: kotlin.collections.List<ProjectDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
