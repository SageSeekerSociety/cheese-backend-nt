package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * Represents a notification with resolved entity information, ready for client-side rendering.
 *
 * @param id Unique identifier for the notification.
 * @param type
 * @param read Indicates if the notification has been read by the recipient.
 * @param createdAt Notification creation timestamp as epoch milliseconds (Unix timestamp * 1000).
 * @param entities Map containing information about entities related to this notification, resolved
 *   by the backend. Keys are logical roles (e.g., \"actor\", \"team\", \"subjectUser\",
 *   \"targetItem\"), and values are the resolved entity details. A value can be null if an entity
 *   reference in the metadata could not be resolved.
 * @param contextMetadata Map containing non-entity specific contextual data needed for rendering,
 *   extracted from the original notification metadata. Examples include the role offered in an
 *   invitation, the specific reaction emoji used, a custom message string, etc.
 */
data class NotificationDTO(
    @Schema(
        example = "101",
        required = true,
        readOnly = true,
        description = "Unique identifier for the notification.",
    )
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: NotificationTypeDTO,
    @Schema(
        example = "false",
        required = true,
        description = "Indicates if the notification has been read by the recipient.",
    )
    @get:JsonProperty("read", required = true)
    val read: kotlin.Boolean,
    @Schema(
        example = "1700000000000",
        required = true,
        readOnly = true,
        description =
            "Notification creation timestamp as epoch milliseconds (Unix timestamp * 1000).",
    )
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @field:Valid
    @Schema(
        example = "null",
        description =
            "Map containing information about entities related to this notification, resolved by the backend. Keys are logical roles (e.g., \"actor\", \"team\", \"subjectUser\", \"targetItem\"), and values are the resolved entity details. A value can be null if an entity reference in the metadata could not be resolved. ",
    )
    @get:JsonProperty("entities")
    val entities: kotlin.collections.Map<kotlin.String, ResolvedEntityInfoDTO>? = null,
    @field:Valid
    @Schema(
        example =
            "{\"role\":\"MEMBER\",\"reactionEmoji\":\"👍\",\"message\":\"Welcome aboard!\",\"someOtherFlag\":true}",
        description =
            "Map containing non-entity specific contextual data needed for rendering, extracted from the original notification metadata. Examples include the role offered in an invitation, the specific reaction emoji used, a custom message string, etc. ",
    )
    @get:JsonProperty("contextMetadata")
    val contextMetadata: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
