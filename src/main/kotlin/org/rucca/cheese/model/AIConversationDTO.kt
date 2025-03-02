package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * AI对话
 *
 * @param id
 * @param conversationId 对话ID
 * @param title 对话标题
 * @param moduleType 模块类型
 * @param contextId 上下文ID
 * @param ownerId 所有者ID
 * @param messageCount 消息数量
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
data class AIConversationDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "对话ID")
    @get:JsonProperty("conversationId", required = true)
    val conversationId: kotlin.String,
    @Schema(example = "null", required = true, description = "对话标题")
    @get:JsonProperty("title", required = true)
    val title: kotlin.String,
    @Schema(example = "null", required = true, description = "模块类型")
    @get:JsonProperty("moduleType", required = true)
    val moduleType: kotlin.String,
    @Schema(example = "null", required = true, description = "上下文ID")
    @get:JsonProperty("contextId", required = true)
    val contextId: kotlin.Long,
    @Schema(example = "null", required = true, description = "所有者ID")
    @get:JsonProperty("ownerId", required = true)
    val ownerId: kotlin.Long,
    @Schema(example = "null", required = true, description = "消息数量")
    @get:JsonProperty("messageCount", required = true)
    val messageCount: kotlin.Int,
    @Schema(example = "null", required = true, description = "创建时间")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: java.time.OffsetDateTime,
    @Schema(example = "null", required = true, description = "更新时间")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: java.time.OffsetDateTime,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
