package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param title
 * @param type
 * @param contentText
 * @param contentAttachment
 */
data class TaskSubmissionContentEntryDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("title", required = true)
    val title: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true)
    val type: TaskSubmissionTypeDTO,
    @Schema(example = "null", description = "")
    @get:JsonProperty("contentText")
    val contentText: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("contentAttachment")
    val contentAttachment: AttachmentDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
