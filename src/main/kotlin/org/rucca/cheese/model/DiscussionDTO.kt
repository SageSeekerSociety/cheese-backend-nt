package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id
 * @param modelType
 * @param modelId 模型ID
 * @param content
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
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("modelType", required = true)
    val modelType: DiscussableModelTypeDTO,
    @Schema(example = "null", required = true, description = "模型ID")
    @get:JsonProperty("modelId", required = true)
    val modelId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: kotlin.String,
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
    val reactions: kotlin.collections.List<DiscussionReactionDTO>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("createdAt")
    val createdAt: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
