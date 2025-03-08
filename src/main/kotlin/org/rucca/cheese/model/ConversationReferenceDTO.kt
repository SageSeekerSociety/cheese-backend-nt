package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param url
 * @param logoUrl
 * @param title
 * @param summary
 * @param publishTime
 * @param extra
 */
data class ConversationReferenceDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("url", required = true)
    val url: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("logo_url", required = true)
    val logoUrl: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("title", required = true)
    val title: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("summary", required = true)
    val summary: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("publish_time", required = true)
    val publishTime: kotlin.String,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("extra", required = true)
    val extra: ConversationReferenceExtraDTO,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
