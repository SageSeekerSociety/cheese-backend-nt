package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param discussions
 * @param page
 */
data class ListDiscussions200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("discussions")
    val discussions: kotlin.collections.List<DiscussionDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("page")
    val page: PageDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
