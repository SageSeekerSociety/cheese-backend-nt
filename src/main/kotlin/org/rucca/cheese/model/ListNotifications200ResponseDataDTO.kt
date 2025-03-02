package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param notifications
 * @param page
 */
data class ListNotifications200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notifications", required = true)
    val notifications: kotlin.collections.List<NotificationDTO>,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("page", required = true)
    val page: PageDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
