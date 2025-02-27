package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param relInfo
 * @param freshnessInfo
 * @param authInfo
 * @param finalRef
 */
data class ConversationReferenceExtraDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("rel_info", required = true)
    val relInfo: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("freshness_info", required = true)
    val freshnessInfo: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("auth_info", required = true)
    val authInfo: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("final_ref", required = true)
    val finalRef: kotlin.String,
) {}
