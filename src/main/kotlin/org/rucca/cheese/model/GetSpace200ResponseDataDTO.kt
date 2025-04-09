package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param space
 * @param categories
 */
data class GetSpace200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("space", required = true)
    val space: SpaceDTO,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("categories")
    val categories: kotlin.collections.List<SpaceCategoryDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
