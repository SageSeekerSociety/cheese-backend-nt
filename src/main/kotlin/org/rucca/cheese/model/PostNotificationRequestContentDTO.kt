package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param text
 * @param projectId
 * @param discussionId
 * @param knowledgeId
 */
data class PostNotificationRequestContentDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("text")
    val text: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("projectId")
    val projectId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("discussionId")
    val discussionId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("knowledgeId")
    val knowledgeId: kotlin.Long? = null,
) {}
