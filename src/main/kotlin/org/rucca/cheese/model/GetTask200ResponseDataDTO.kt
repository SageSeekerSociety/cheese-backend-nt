package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param task
 * @param participation
 */
data class GetTask200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("task", required = true)
    val task: TaskDTO,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("participation", required = true)
    val participation: TaskParticipationInfoDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
