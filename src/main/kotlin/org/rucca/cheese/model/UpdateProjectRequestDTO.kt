package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.constraints.Pattern

/**
 * @param name
 * @param description
 * @param colorCode
 * @param startDate
 * @param endDate
 * @param teamId
 * @param leaderId
 * @param content
 */
data class UpdateProjectRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @get:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Schema(example = "null", description = "")
    @get:JsonProperty("colorCode")
    val colorCode: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("startDate")
    val startDate: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("endDate")
    val endDate: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("teamId")
    val teamId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("leaderId")
    val leaderId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
