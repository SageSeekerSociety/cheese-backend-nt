package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param id
 * @param type
 * @param receiverId
 * @param content
 */
data class NotificationsGet200ResponseDataNotificationsDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("id")
    val id: kotlin.Long? = 7001L,
    @Schema(example = "null", description = "")
    @get:JsonProperty("type")
    val type: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("receiverId")
    val receiverId: kotlin.Long? = 1002L,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: NotificationsGet200ResponseDataNotificationsContentDTO? = null
) {}
