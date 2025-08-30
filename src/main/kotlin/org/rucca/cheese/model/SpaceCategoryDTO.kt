package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param id Unique identifier for the category.
 * @param name Name of the category. Must be unique within the space.
 * @param displayOrder Order in which the category should be displayed. Lower numbers typically come
 *   first.
 * @param createdAt Category creation timestamp (epoch milliseconds).
 * @param updatedAt Category last update timestamp (epoch milliseconds).
 * @param description Optional description for the category.
 * @param archivedAt Timestamp when the category was archived (epoch milliseconds). Null if not
 *   archived.
 */
data class SpaceCategoryDTO(
    @Schema(
        example = "null",
        required = true,
        readOnly = true,
        description = "Unique identifier for the category.",
    )
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @Schema(
        example = "Backend Development",
        required = true,
        description = "Name of the category. Must be unique within the space.",
    )
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(
        example = "10",
        required = true,
        description =
            "Order in which the category should be displayed. Lower numbers typically come first.",
    )
    @get:JsonProperty("displayOrder", required = true)
    val displayOrder: kotlin.Int = 0,
    @Schema(
        example = "null",
        required = true,
        readOnly = true,
        description = "Category creation timestamp (epoch milliseconds).",
    )
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(
        example = "null",
        required = true,
        readOnly = true,
        description = "Category last update timestamp (epoch milliseconds).",
    )
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @Schema(
        example = "Tasks related to server-side logic and databases.",
        description = "Optional description for the category.",
    )
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @Schema(
        example = "null",
        description =
            "Timestamp when the category was archived (epoch milliseconds). Null if not archived.",
    )
    @get:JsonProperty("archivedAt")
    val archivedAt: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
