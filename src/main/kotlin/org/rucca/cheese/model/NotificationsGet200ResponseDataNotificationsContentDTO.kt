package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param text
 * @param projectId
 * @param discussionId
 */
data class NotificationsGet200ResponseDataNotificationsContentDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("text")
    val text: kotlin.String? = "Hello, World!",
    @Schema(example = "null", description = "")
    @get:JsonProperty("projectId")
    val projectId: kotlin.Long? = 2002L,
    @Schema(example = "null", description = "")
    @get:JsonProperty("discussionId")
    val discussionId: kotlin.Long? = 5001L
) {}
