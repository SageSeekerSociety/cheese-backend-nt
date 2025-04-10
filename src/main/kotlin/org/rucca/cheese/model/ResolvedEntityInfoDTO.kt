package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * Represents basic display information for a resolved entity (User, Team, Project, etc.).
 *
 * @param id Unique identifier of the resolved entity.
 * @param type The type of the entity (e.g., 'user', 'team', 'project'). Helps the client understand
 *   the context.
 * @param name The display name (e.g., nickname, team name, project title) of the entity.
 * @param url A URL pointing to the entity's page or resource, if applicable.
 * @param avatarUrl A URL pointing to an avatar, logo, or icon for the entity, if applicable.
 * @param status The status of the entity (e.g., 'online', 'offline', 'busy'), if applicable.
 */
data class ResolvedEntityInfoDTO(
    @Schema(
        example = "123",
        required = true,
        description = "Unique identifier of the resolved entity.",
    )
    @get:JsonProperty("id", required = true)
    val id: kotlin.String,
    @Schema(
        example = "user",
        required = true,
        description =
            "The type of the entity (e.g., 'user', 'team', 'project'). Helps the client understand the context.",
    )
    @get:JsonProperty("type", required = true)
    val type: kotlin.String,
    @Schema(
        example = "Alice",
        required = true,
        description = "The display name (e.g., nickname, team name, project title) of the entity.",
    )
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(
        example = "/users/123",
        description = "A URL pointing to the entity's page or resource, if applicable.",
    )
    @get:JsonProperty("url")
    val url: kotlin.String? = null,
    @Schema(
        example = "https://example.com/avatars/123.png",
        description = "A URL pointing to an avatar, logo, or icon for the entity, if applicable.",
    )
    @get:JsonProperty("avatarUrl")
    val avatarUrl: kotlin.String? = null,
    @Schema(
        example = "online",
        description = "The status of the entity (e.g., 'online', 'offline', 'busy'), if applicable.",
    )
    @get:JsonProperty("status")
    val status: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
