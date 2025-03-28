package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param id
 * @param member
 * @param createdAt
 * @param updatedAt
 * @param approved
 * @param deadline
 * @param realNameInfo
 * @param applyReason
 * @param personalAdvantage
 * @param remark
 */
data class TaskMembershipDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true)
    val id: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("member", required = true)
    val member: TaskParticipantSummaryDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true)
    val createdAt: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true)
    val updatedAt: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("approved", required = true)
    val approved: ApproveTypeDTO,
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("realNameInfo")
    val realNameInfo: TaskParticipantRealNameInfoDTO? = null,
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
