package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id 对话ID
 * @param question 用户问题
 * @param response 回答内容
 * @param modelType 模型类型
 * @param followupQuestions 后续问题
 * @param createdAt 会话创建时间
 * @param tokensUsed 消耗的token数量
 * @param seuConsumed 消耗的SEU数量
 * @param reasoningContent 推理内容
 * @param reasoningTimeMs 推理所用时间（毫秒）
 * @param references 参考文献
 * @param conversationId 会话ID
 * @param parentId 父消息ID
 */
data class TaskAIAdviceConversationDTO(
    @Schema(example = "null", required = true, description = "对话ID")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
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
    @Schema(example = "100", required = true, description = "消耗的token数量")
    @get:JsonProperty("tokensUsed", required = true)
    val tokensUsed: kotlin.Int,
    @Schema(example = "0.1", required = true, description = "消耗的SEU数量")
    @get:JsonProperty("seuConsumed", required = true)
    val seuConsumed: kotlin.Double,
    @Schema(example = "null", description = "推理内容")
    @get:JsonProperty("reasoningContent")
    val reasoningContent: kotlin.String? = null,
    @Schema(example = "1000", description = "推理所用时间（毫秒）")
    @get:JsonProperty("reasoningTimeMs")
    val reasoningTimeMs: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "null", description = "参考文献")
    @get:JsonProperty("references")
    val references: kotlin.collections.List<ConversationReferenceDTO>? = null,
    @Schema(example = "null", description = "会话ID")
    @get:JsonProperty("conversationId")
    val conversationId: kotlin.String? = null,
    @Schema(example = "null", description = "父消息ID")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
) {}
