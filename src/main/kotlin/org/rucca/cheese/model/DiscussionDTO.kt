package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id
 * @param content
 * @param modelType 模型类型
 * @param modelId 模型ID
 * @param parentId 回复的讨论ID
 * @param sender
 * @param mentionedUsers
 * @param reactions
 * @param createdAt
 */
data class DiscussionDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
    @Schema(example = "null", description = "模型类型")
    @get:JsonProperty("modelType")
    val modelType: DiscussionDTO.ModelType? = null,
    @Schema(example = "null", description = "模型ID")
    @get:JsonProperty("modelId")
    val modelId: kotlin.Long? = null,
    @Schema(example = "null", description = "回复的讨论ID")
    @get:JsonProperty("parentId")
    val parentId: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("sender")
    val sender: UserDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("mentionedUsers")
    val mentionedUsers: kotlin.collections.List<UserDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("reactions")
    val reactions:
        kotlin.collections.List<DiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO>? =
        null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("createdAt")
    val createdAt: kotlin.Long? = null,
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
