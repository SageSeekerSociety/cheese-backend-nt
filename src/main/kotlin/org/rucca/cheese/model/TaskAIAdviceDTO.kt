package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.io.Serializable

/**
 * @param topicSummary
 * @param knowledgeFields
 * @param learningPaths
 * @param methodology
 * @param teamTips
 */
data class TaskAIAdviceDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("topic_summary")
    val topicSummary: TaskAIAdviceTopicSummaryDTO? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("knowledge_fields")
    val knowledgeFields: kotlin.collections.List<TaskAIAdviceKnowledgeFieldsInnerDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("learning_paths")
    val learningPaths: kotlin.collections.List<TaskAIAdviceLearningPathsInnerDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("methodology")
    val methodology: kotlin.collections.List<TaskAIAdviceMethodologyInnerDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("team_tips")
    val teamTips: kotlin.collections.List<TaskAIAdviceTeamTipsInnerDTO>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
