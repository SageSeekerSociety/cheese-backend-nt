package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param question 用户问题
 * @param context
 * @param modelType 可选的模型类型（例如：standard、reasoning）
 * @param conversationId 可选的会话ID（用于继续对话）
 * @param parentId 可选的父消息ID（用于继续特定消息的对话）
 */
data class CreateTaskAIAdviceConversationRequestDTO(
    @Schema(example = "null", required = true, description = "用户问题")
    @get:JsonProperty("question", required = true)
    val question: kotlin.String,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("context")
    val context: TaskAIAdviceConversationContextDTO? = null,
    @Schema(example = "null", description = "可选的模型类型（例如：standard、reasoning）")
    @get:JsonProperty("modelType")
    val modelType: kotlin.String? = "standard",
    @Schema(example = "null", description = "可选的会话ID（用于继续对话）")
    @get:JsonProperty("conversationId")
    val conversationId: kotlin.String? = null,
    @Schema(example = "null", description = "可选的父消息ID（用于继续特定消息的对话）")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
