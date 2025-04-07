package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/** @param notification */
data class GetNotificationById200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notification", required = true)
    val notification: NotificationDTO
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
