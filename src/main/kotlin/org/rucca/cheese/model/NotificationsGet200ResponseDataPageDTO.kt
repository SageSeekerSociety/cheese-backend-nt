package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param pageStart
 * @param pageSize
 * @param hasPrev
 * @param prevStart
 * @param hasMore
 * @param nextStart
 */
data class NotificationsGet200ResponseDataPageDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("page_start")
    val pageStart: kotlin.Int? = 7001,
    @Schema(example = "null", description = "")
    @get:JsonProperty("page_size")
    val pageSize: kotlin.Int? = 20,
    @Schema(example = "null", description = "")
    @get:JsonProperty("has_prev")
    val hasPrev: kotlin.Boolean? = false,
    @Schema(example = "null", description = "")
    @get:JsonProperty("prev_start")
    val prevStart: kotlin.Int? = 0,
    @Schema(example = "null", description = "")
    @get:JsonProperty("has_more")
    val hasMore: kotlin.Boolean? = true,
    @Schema(example = "null", description = "")
    @get:JsonProperty("next_start")
    val nextStart: kotlin.Int? = 7020
) {}
