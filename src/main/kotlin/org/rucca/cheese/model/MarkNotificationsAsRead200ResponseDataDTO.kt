package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param notificationIds */
data class MarkNotificationsAsRead200ResponseDataDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("notificationIds")
    val notificationIds: kotlin.collections.List<kotlin.Long>? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
