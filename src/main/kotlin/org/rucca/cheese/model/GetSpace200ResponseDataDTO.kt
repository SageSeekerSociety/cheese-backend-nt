package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/** @param space */
data class GetSpace200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("space", required = true)
    val space: SpaceDTO
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
