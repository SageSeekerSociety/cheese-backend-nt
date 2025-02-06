package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param contentText
 * @param contentAttachmentId
 */
data class PostTaskSubmissionRequestInnerDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("contentText")
    val contentText: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("contentAttachmentId")
    val contentAttachmentId: kotlin.Long? = null,
) {}
