package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param text
 * @param attachmentId
 */
data class TaskSubmissionContentDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("text")
    val text: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("attachmentId")
    val attachmentId: kotlin.Long? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
