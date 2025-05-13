package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param count Total number of unread notifications. */
data class GetUnreadNotificationsCount200ResponseDataDTO(
    @Schema(
        example = "null",
        required = true,
        description = "Total number of unread notifications.",
    )
    @get:JsonProperty("count", required = true)
    val count: kotlin.Long
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
