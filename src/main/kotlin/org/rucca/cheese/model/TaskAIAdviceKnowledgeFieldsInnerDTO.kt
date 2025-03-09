package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param name
 * @param description
 * @param followupQuestions
 */
data class TaskAIAdviceKnowledgeFieldsInnerDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true)
    val name: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("description", required = true)
    val description: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("followup_questions", required = true)
    val followupQuestions: kotlin.collections.List<kotlin.String>,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
