package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/** @param categories */
data class ListSpaceCategories200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("categories", required = true)
    val categories: kotlin.collections.List<SpaceCategoryDTO>
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
