package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.constraints.Size

/**
 * @param name New name for the category. Must be unique within the space if changed.
 * @param description New description for the category. Set to null to clear.
 * @param displayOrder New display order for the category.
 */
data class UpdateSpaceCategoryRequestDTO(
    @get:Size(min = 1)
    @Schema(
        example = "High Priority",
        description = "New name for the category. Must be unique within the space if changed.",
    )
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(
        example = "Tasks requiring immediate attention.",
        description = "New description for the category. Set to null to clear.",
    )
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(example = "2", description = "New display order for the category.")
    @get:JsonProperty("displayOrder")
    val displayOrder: kotlin.Int? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
