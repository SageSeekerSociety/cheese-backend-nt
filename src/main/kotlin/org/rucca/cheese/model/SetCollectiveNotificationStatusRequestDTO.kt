package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param read Must be 'true' to mark all as read. Setting to 'false' is not supported via this
 *   endpoint.
 */
data class SetCollectiveNotificationStatusRequestDTO(
    @Schema(
        example = "null",
        required = true,
        description =
            "Must be 'true' to mark all as read. Setting to 'false' is not supported via this endpoint.",
    )
    @get:JsonProperty("read", required = true)
    val read: kotlin.Boolean
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
