package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param knowledge
 * @param page
 */
data class KnowledgeGet200ResponseDataDTO(
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("knowledge")
    val knowledge: kotlin.collections.List<KnowledgeDTO>? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("page")
    val page: PageDTO? = null,
) {}
