package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * 用户AI配额信息
 *
 * @param remaining 剩余可用SEU
 * @param total 总可用SEU
 * @param resetTime 配额重置时间
 */
data class QuotaInfoDTO(
    @Schema(example = "null", required = true, description = "剩余可用SEU")
    @get:JsonProperty("remaining", required = true)
    val remaining: kotlin.Double,
    @Schema(example = "null", required = true, description = "总可用SEU")
    @get:JsonProperty("total", required = true)
    val total: kotlin.Double,
    @Schema(example = "null", required = true, description = "配额重置时间")
    @get:JsonProperty("reset_time", required = true)
    val resetTime: java.time.OffsetDateTime,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
