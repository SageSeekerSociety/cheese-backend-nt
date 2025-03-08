package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param conversationId 会话组ID
 * @param createdAt 会话创建时间
 * @param updatedAt 会话最后更新时间
 * @param messageCount 会话中的消息数量
 * @param title 会话组标题
 * @param latestMessage
 */
data class ConversationGroupSummaryDTO(
    @Schema(example = "null", required = true, description = "会话组ID")
    @get:JsonProperty("conversationId", required = true)
    val conversationId: kotlin.String,
    @Schema(example = "null", required = true, description = "会话创建时间")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: java.time.OffsetDateTime,
    @Schema(example = "null", required = true, description = "会话最后更新时间")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: java.time.OffsetDateTime,
    @Schema(example = "null", required = true, description = "会话中的消息数量")
    @get:JsonProperty("messageCount", required = true)
    val messageCount: kotlin.Int,
    @Schema(example = "机器学习模型选择", description = "会话组标题")
    @get:JsonProperty("title")
    val title: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("latestMessage")
    val latestMessage: TaskAIAdviceConversationDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
