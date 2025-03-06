package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.constraints.Pattern

/**
 * @param id
 * @param name
 * @param description
 * @param startDate 项目开始时间戳(毫秒)
 * @param endDate 项目结束时间戳(毫秒)
 * @param leaderId
 * @param content
 * @param colorCode
 * @param parentId 父项目ID
 * @param externalTaskId
 * @param githubRepo
 * @param createdAt
 * @param updatedAt
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
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("leaderId", required = true)
    val leaderId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
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
    @get:JsonProperty("createdAt")
    val createdAt: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("updatedAt")
    val updatedAt: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
