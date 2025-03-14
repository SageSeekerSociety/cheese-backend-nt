package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param content
 * @param parentId 回复某条讨论 (可选)
 * @param mentionedUserIds 提及的用户ID (可选)
 * @param modelType 模型类型
 * @param modelId 模型ID
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
    @Schema(example = "null", description = "模型类型")
    @get:JsonProperty("modelType")
    val modelType: DiscussionsPostRequestDTO.ModelType? = null,
    @Schema(example = "null", description = "模型ID")
    @get:JsonProperty("modelId")
    val modelId: kotlin.Long? = null,
) : Serializable {

    /** 模型类型 Values: PROJECT */
    enum class ModelType(@get:JsonValue val value: kotlin.String) {

        PROJECT("PROJECT");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): ModelType {
                return values().first { it -> it.value == value }
            }
        }
    }

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
