package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** @param notificationIds */
data class MarkNotificationsAsRead200ResponseDataDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("notificationIds")
    val notificationIds: kotlin.collections.List<kotlin.Long>? = null
) {}
