package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/** @param knowledge */
data class UpdateKnowledge200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("knowledge", required = true)
    val knowledge: KnowledgeDTO
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
