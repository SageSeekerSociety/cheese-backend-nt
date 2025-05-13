package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** @param updatedIds List of notification IDs successfully updated. */
data class BulkUpdateNotifications200ResponseDataDTO(
    @Schema(example = "null", description = "List of notification IDs successfully updated.")
    @get:JsonProperty("updatedIds")
    val updatedIds: kotlin.collections.List<kotlin.Long>? = null
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
