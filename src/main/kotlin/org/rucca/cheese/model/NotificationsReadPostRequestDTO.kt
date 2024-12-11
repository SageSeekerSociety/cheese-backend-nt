package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** @param notificationIds */
data class NotificationsReadPostRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notificationIds", required = true)
    val notificationIds: kotlin.collections.List<kotlin.Long>
) {}
