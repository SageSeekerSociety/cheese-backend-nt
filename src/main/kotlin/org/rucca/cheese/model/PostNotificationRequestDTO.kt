package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param type
 * @param receiverId
 * @param content
 */
data class PostNotificationRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: PostNotificationRequestDTO.Type,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("receiverId", required = true)
    val receiverId: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("content", required = true)
    val content: PostNotificationRequestContentDTO,
) {

    /** Values: mention,reply,reaction,project_invite,deadline_remind */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        mention("mention"),
        reply("reply"),
        reaction("reaction"),
        project_invite("project_invite"),
        deadline_remind("deadline_remind");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().first { it -> it.value == value }
            }
        }
    }
}
