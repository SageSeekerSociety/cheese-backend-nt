package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param count The number of notifications marked as read. */
data class SetCollectiveNotificationStatus200ResponseDataDTO(
    @Schema(example = "null", description = "The number of notifications marked as read.")
    @get:JsonProperty("count")
    val count: kotlin.Int? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
