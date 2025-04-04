package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id
 * @param type
 * @param receiverId The ID of the user who will receive the notification. This will be mapped to a
 *   User entity in the backend.
 * @param content
 * @param read
 * @param createdAt
 */
data class NotificationDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: NotificationDTO.Type,
    @Schema(
        example = "null",
        required = true,
        description =
            "The ID of the user who will receive the notification. This will be mapped to a User entity in the backend.",
    )
    @get:JsonProperty("receiverId", required = true)
    val receiverId: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: NotificationContentDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("read", required = true)
    val read: kotlin.Boolean = false,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
) : Serializable {

    /** Values: MENTION,REPLY,REACTION,PROJECT_INVITE,DEADLINE_REMIND */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        MENTION("MENTION"),
        REPLY("REPLY"),
        REACTION("REACTION"),
        PROJECT_INVITE("PROJECT_INVITE"),
        DEADLINE_REMIND("DEADLINE_REMIND");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().first { it -> it.value == value }
            }
        }
    }

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
