package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.constraints.Size

/**
 * @param name Name for the new category. Must be unique within the space.
 * @param description Optional description for the category.
 * @param displayOrder Optional display order. Defaults to 0 if not provided.
 */
data class CreateSpaceCategoryRequestDTO(
    @get:Size(min = 1)
    @Schema(
        example = "Urgent",
        required = true,
        description = "Name for the new category. Must be unique within the space.",
    )
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(
        example = "High priority tasks.",
        description = "Optional description for the category.",
    )
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(example = "5", description = "Optional display order. Defaults to 0 if not provided.")
    @get:JsonProperty("displayOrder")
    val displayOrder: kotlin.Int? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
