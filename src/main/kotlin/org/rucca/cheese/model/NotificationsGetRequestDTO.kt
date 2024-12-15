package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param type
 * @param pageStart
 * @param pageSize
 * @param read
 */
data class NotificationsGetRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: NotificationsGetRequestDTO.Type,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("page_start", required = true)
    val pageStart: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("page_size", required = true)
    val pageSize: kotlin.Int,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("read", required = true)
    val read: kotlin.Boolean
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
