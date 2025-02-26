package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * AI消息
 *
 * @param id 消息ID
 * @param conversationId 对话ID
 * @param role 角色
 * @param content 消息内容
 * @param createdAt 创建时间
 * @param parentId 父消息ID
 * @param reasoningContent 推理内容
 * @param reasoningTimeMs 推理时间（毫秒）
 * @param metadata 元数据
 */
data class AIMessageDTO(
    @Schema(example = "null", required = true, description = "消息ID")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "对话ID")
    @get:JsonProperty("conversationId", required = true)
    val conversationId: kotlin.String,
    @Schema(example = "null", required = true, description = "角色")
    @get:JsonProperty("role", required = true)
    val role: kotlin.String,
    @Schema(example = "null", required = true, description = "消息内容")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
    @Schema(example = "null", required = true, description = "创建时间")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: java.time.OffsetDateTime,
    @Schema(example = "null", description = "父消息ID")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
    @Schema(example = "null", description = "推理内容")
    @get:JsonProperty("reasoningContent")
    val reasoningContent: kotlin.String? = null,
    @Schema(example = "null", description = "推理时间（毫秒）")
    @get:JsonProperty("reasoningTimeMs")
    val reasoningTimeMs: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "{\"followupQuestions\":[\"问题1\",\"问题2\"]}", description = "元数据")
    @get:JsonProperty("metadata")
    val metadata: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,
) {}
