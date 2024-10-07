package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 分页信息
 *
 * @param pageStart 该页第一个 item 的 ID
 * @param pageSize 每页 item 数量
 * @param hasPrev 是否有上一页
 * @param hasMore 是否有下一页
 * @param prevStart 上一页第一个 item 的 ID
 * @param nextStart 下一页第一个 item 的 ID
 */
data class PageDTO(
    @Schema(example = "null", required = true, description = "该页第一个 item 的 ID")
    @get:JsonProperty("page_start", required = true)
    val pageStart: kotlin.Long,
    @Schema(example = "null", required = true, description = "每页 item 数量")
    @get:JsonProperty("page_size", required = true)
    val pageSize: kotlin.Int,
    @Schema(example = "null", required = true, description = "是否有上一页")
    @get:JsonProperty("has_prev", required = true)
    val hasPrev: kotlin.Boolean,
    @Schema(example = "null", required = true, description = "是否有下一页")
    @get:JsonProperty("has_more", required = true)
    val hasMore: kotlin.Boolean,
    @Schema(example = "null", description = "上一页第一个 item 的 ID")
    @get:JsonProperty("prev_start")
    val prevStart: kotlin.Long? = null,
    @Schema(example = "null", description = "下一页第一个 item 的 ID")
    @get:JsonProperty("next_start")
    val nextStart: kotlin.Long? = null
) {}
