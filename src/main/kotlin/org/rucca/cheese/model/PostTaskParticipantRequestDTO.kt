package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param deadline
 * @param realNameInfo
 */
data class PostTaskParticipantRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("deadline")
    val deadline: kotlin.Long? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("realNameInfo")
    val realNameInfo: TaskParticipantRealNameInfoDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
