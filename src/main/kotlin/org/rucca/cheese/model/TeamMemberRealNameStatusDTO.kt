package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param memberId
 * @param hasRealNameInfo
 * @param userName
 */
data class TeamMemberRealNameStatusDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("memberId", required = true)
    val memberId: kotlin.Long,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("hasRealNameInfo", required = true)
    val hasRealNameInfo: kotlin.Boolean,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("userName", required = true)
    val userName: kotlin.String,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
