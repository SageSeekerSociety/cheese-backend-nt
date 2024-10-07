package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param spaces
 * @param page
 */
data class GetSpaces200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("spaces")
    val spaces: kotlin.collections.List<SpaceDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("page")
    val page: PageDTO? = null
) {}
