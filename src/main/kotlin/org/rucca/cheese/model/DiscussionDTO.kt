package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param projectId
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
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("projectId", required = true)
    val projectId: kotlin.Long,
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
    val reactions:
        kotlin.collections.List<
            ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO
        >? =
        null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("createdAt")
    val createdAt: kotlin.Long? = null,
) {}
