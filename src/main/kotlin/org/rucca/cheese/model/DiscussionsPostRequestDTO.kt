package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param content
 * @param parentId 回复某条讨论 (可选)
 * @param mentionedUserIds 提及的用户ID (可选)
 * @param projectId 项目ID
 */
data class DiscussionsPostRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
    @Schema(example = "null", description = "回复某条讨论 (可选)")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
    @Schema(example = "null", description = "提及的用户ID (可选)")
    @get:JsonProperty("mentionedUserIds")
    val mentionedUserIds: kotlin.collections.List<kotlin.Long>? = null,
    @Schema(example = "null", description = "项目ID")
    @get:JsonProperty("projectId")
    val projectId: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
