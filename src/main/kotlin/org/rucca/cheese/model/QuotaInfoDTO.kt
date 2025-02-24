package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 用户AI配额信息
 *
 * @param remaining 剩余可用次数
 * @param total 总可用次数
 * @param resetTime 配额重置时间
 */
data class QuotaInfoDTO(
    @Schema(example = "null", required = true, description = "剩余可用次数")
    @get:JsonProperty("remaining", required = true)
    val remaining: kotlin.Int,
    @Schema(example = "null", required = true, description = "总可用次数")
    @get:JsonProperty("total", required = true)
    val total: kotlin.Int,
    @Schema(example = "null", required = true, description = "配额重置时间")
    @get:JsonProperty("reset_time", required = true)
    val resetTime: java.time.OffsetDateTime,
) {}
