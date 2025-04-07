package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid
import javax.validation.constraints.Size

/** @param updates */
data class BulkUpdateNotificationsRequestDTO(
    @field:Valid
    @get:Size(min = 1)
    @Schema(
        example = "[{\"id\":123,\"read\":true},{\"id\":456,\"read\":true}]",
        required = true,
        description = "",
    )
    @get:JsonProperty("updates", required = true)
    val updates: kotlin.collections.List<BulkUpdateNotificationsRequestUpdatesInnerDTO>
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
