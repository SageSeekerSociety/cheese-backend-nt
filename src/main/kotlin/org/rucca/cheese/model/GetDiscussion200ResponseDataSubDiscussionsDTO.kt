package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param discussions
 * @param page
 */
data class GetDiscussion200ResponseDataSubDiscussionsDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("discussions", required = true)
    val discussions: kotlin.collections.List<DiscussionDTO>,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("page", required = true)
    val page: PageDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
