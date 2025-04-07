package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param id The ID of the notification to update.
 * @param read The desired read status.
 */
data class BulkUpdateNotificationsRequestUpdatesInnerDTO(
    @Schema(
        example = "null",
        required = true,
        description = "The ID of the notification to update.",
    )
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(example = "null", required = true, description = "The desired read status.")
    @get:JsonProperty("read", required = true)
    val read: kotlin.Boolean,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
