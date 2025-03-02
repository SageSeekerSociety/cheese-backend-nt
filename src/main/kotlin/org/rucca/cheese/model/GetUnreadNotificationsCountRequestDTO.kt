package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param receiverId */
data class GetUnreadNotificationsCountRequestDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("receiverId", required = true)
    val receiverId: kotlin.Long
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
