package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param name
 * @param type
 * @param content
 * @param teamId 知识条目所属的团队ID
 * @param description
 * @param projectId 相关的项目ID（可选）
 * @param materialId 来源 Material ID（可选）
 * @param discussionId 来源讨论ID（可选）
 * @param labels
 */
data class CreateKnowledgeRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: KnowledgeTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
    @Schema(example = "null", required = true, description = "知识条目所属的团队ID")
    @get:JsonProperty("teamId", required = true)
    val teamId: kotlin.Long,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(example = "null", description = "相关的项目ID（可选）")
    @get:JsonProperty("projectId")
    val projectId: kotlin.Long? = null,
    @Schema(example = "null", description = "来源 Material ID（可选）")
    @get:JsonProperty("materialId")
    val materialId: kotlin.Long? = null,
    @Schema(example = "null", description = "来源讨论ID（可选）")
    @get:JsonProperty("discussionId")
    val discussionId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("labels")
    val labels: kotlin.collections.List<kotlin.String>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
