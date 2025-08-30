package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id
 * @param type
 * @param memberId
 * @param canSubmit
 * @param approved
 * @param teamName
 */
data class TaskParticipationIdentityDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: TaskSubmitterTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("memberId", required = true)
    val memberId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("canSubmit", required = true)
    val canSubmit: kotlin.Boolean,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("approved", required = true)
    val approved: ApproveTypeDTO,
    @Schema(example = "null", description = "")
    @get:JsonProperty("teamName")
    val teamName: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
