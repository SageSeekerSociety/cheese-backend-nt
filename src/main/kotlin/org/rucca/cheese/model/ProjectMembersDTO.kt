package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param count
 * @param examples
 */
data class ProjectMembersDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("count")
    val count: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("examples")
    val examples: kotlin.collections.List<ProjectMembershipDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
