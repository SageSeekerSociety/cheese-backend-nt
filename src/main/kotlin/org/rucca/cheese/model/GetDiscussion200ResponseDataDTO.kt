package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param discussion
 * @param subDiscussions
 */
data class GetDiscussion200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("discussion", required = true)
    val discussion: DiscussionDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("subDiscussions", required = true)
    val subDiscussions: GetDiscussion200ResponseDataSubDiscussionsDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
