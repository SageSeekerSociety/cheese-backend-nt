package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param id
 * @param name
 * @param type
 * @param content
 * @param description
 * @param material
 * @param teamId 知识条目所属的团队ID
 * @param projectId 相关的项目ID（可选）
 * @param sourceType 知识来源类型：MANUAL或FROM_DISCUSSION
 * @param discussionId 来源讨论ID（如果从讨论中添加）
 * @param labels
 * @param creator
 * @param createdAt
 * @param updatedAt
 */
data class KnowledgeDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
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
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("material")
    val material: MaterialDTO? = null,
    @Schema(example = "null", description = "知识条目所属的团队ID")
    @get:JsonProperty("teamId")
    val teamId: kotlin.Long? = null,
    @Schema(example = "null", description = "相关的项目ID（可选）")
    @get:JsonProperty("projectId")
    val projectId: kotlin.Long? = null,
    @Schema(example = "null", description = "知识来源类型：MANUAL或FROM_DISCUSSION")
    @get:JsonProperty("sourceType")
    val sourceType: kotlin.String? = null,
    @Schema(example = "null", description = "来源讨论ID（如果从讨论中添加）")
    @get:JsonProperty("discussionId")
    val discussionId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("labels")
    val labels: kotlin.collections.List<kotlin.String>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("creator")
    val creator: UserDTO? = null,
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
