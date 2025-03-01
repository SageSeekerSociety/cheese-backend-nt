package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/** @param count */
data class GetUnreadNotificationsCount200ResponseDataDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("count")
    val count: kotlin.Int? = 0
) {}
