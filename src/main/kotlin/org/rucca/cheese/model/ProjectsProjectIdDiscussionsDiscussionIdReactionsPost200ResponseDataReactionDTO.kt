package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param emoji
 * @param count
 * @param users
 */
data class ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("emoji")
    val emoji: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("count")
    val count: kotlin.Int? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("users")
    val users: kotlin.collections.List<UserDTO>? = null,
) {}
