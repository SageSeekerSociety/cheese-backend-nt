package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param id 对话ID
 * @param taskId 任务ID
 * @param question 用户问题
 * @param response 回答内容
 * @param modelType 模型类型
 * @param followupQuestions 后续问题
 * @param createdAt 会话创建时间
 * @param title 对话标题
 * @param reasoningContent 推理内容
 * @param reasoningTimeMs 推理所用时间（毫秒）
 * @param conversationId 会话ID
 * @param parentId 父消息ID
 */
data class TaskAIAdviceConversationDTO(
    @Schema(example = "null", required = true, description = "对话ID")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "任务ID")
    @get:JsonProperty("taskId", required = true)
    val taskId: kotlin.Long,
    @Schema(example = "null", required = true, description = "用户问题")
    @get:JsonProperty("question", required = true)
    val question: kotlin.String,
    @Schema(example = "null", required = true, description = "回答内容")
    @get:JsonProperty("response", required = true)
    val response: kotlin.String,
    @Schema(example = "null", required = true, description = "模型类型")
    @get:JsonProperty("modelType", required = true)
    val modelType: kotlin.String,
    @Schema(example = "null", required = true, description = "后续问题")
    @get:JsonProperty("followupQuestions", required = true)
    val followupQuestions: kotlin.collections.List<kotlin.String>,
    @Schema(example = "null", required = true, description = "会话创建时间")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: java.time.OffsetDateTime,
    @Schema(example = "机器学习模型选择", description = "对话标题")
    @get:JsonProperty("title")
    val title: kotlin.String? = null,
    @Schema(example = "null", description = "推理内容")
    @get:JsonProperty("reasoningContent")
    val reasoningContent: kotlin.String? = null,
    @Schema(example = "1000", description = "推理所用时间（毫秒）")
    @get:JsonProperty("reasoningTimeMs")
    val reasoningTimeMs: kotlin.Long? = null,
    @Schema(example = "null", description = "会话ID")
    @get:JsonProperty("conversationId")
    val conversationId: kotlin.String? = null,
    @Schema(example = "null", description = "父消息ID")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
) {}
