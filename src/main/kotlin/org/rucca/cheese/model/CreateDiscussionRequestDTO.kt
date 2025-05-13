package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param content
 * @param modelType
 * @param modelId 模型ID
 * @param parentId 回复某条讨论 (可选)
 * @param mentionedUserIds 提及的用户ID (可选)
 */
data class CreateDiscussionRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("modelType", required = true)
    val modelType: DiscussableModelTypeDTO,
    @Schema(example = "null", required = true, description = "模型ID")
    @get:JsonProperty("modelId", required = true)
    val modelId: kotlin.Long,
    @Schema(example = "null", description = "回复某条讨论 (可选)")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
    @Schema(example = "null", description = "提及的用户ID (可选)")
    @get:JsonProperty("mentionedUserIds")
    val mentionedUserIds: kotlin.collections.List<kotlin.Long>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
