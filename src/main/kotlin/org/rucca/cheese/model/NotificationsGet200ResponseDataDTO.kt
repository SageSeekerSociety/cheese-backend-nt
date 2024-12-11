package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param notifications
 * @param page
 */
data class NotificationsGet200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("notifications", required = true)
    val notifications: NotificationsGet200ResponseDataNotificationsDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("page", required = true)
    val page: NotificationsGet200ResponseDataPageDTO
) {}
