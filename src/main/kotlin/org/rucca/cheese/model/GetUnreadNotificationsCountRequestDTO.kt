package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** @param receiverId */
data class GetUnreadNotificationsCountRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("receiverId", required = true)
    val receiverId: kotlin.Long
) {}
