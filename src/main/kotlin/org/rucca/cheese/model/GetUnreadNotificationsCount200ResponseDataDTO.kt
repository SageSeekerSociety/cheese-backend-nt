package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param count */
data class GetUnreadNotificationsCount200ResponseDataDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("count")
    val count: kotlin.Int? = 0
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
