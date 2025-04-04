package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param deadline
 * @param email
 * @param phone
 * @param applyReason
 * @param personalAdvantage
 * @param remark
 */
data class PostTaskParticipantRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("email")
    val email: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("phone")
    val phone: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("applyReason")
    val applyReason: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("personalAdvantage")
    val personalAdvantage: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("remark")
    val remark: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
