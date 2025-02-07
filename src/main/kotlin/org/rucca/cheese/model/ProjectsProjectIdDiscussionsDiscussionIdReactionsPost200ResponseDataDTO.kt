package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/** @param reaction */
data class ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("reaction")
    val reaction: ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO? =
        null
) {}
