package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * include owner
 *
 * @param total
 * @param examples
 */
data class TeamAdminsDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("total", required = true)
    val total: kotlin.Int,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("examples", required = true)
    val examples: kotlin.collections.List<UserDTO>,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
