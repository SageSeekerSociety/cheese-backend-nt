package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param notificationIds */
data class MarkNotificationsAsReadRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notificationIds", required = true)
    val notificationIds: kotlin.collections.List<kotlin.Long>
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
