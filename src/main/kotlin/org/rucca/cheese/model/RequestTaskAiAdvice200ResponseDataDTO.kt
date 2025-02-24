package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * @param status
 * @param quota
 */
data class RequestTaskAiAdvice200ResponseDataDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("status")
    val status: RequestTaskAiAdvice200ResponseDataDTO.Status? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("quota")
    val quota: QuotaInfoDTO? = null,
) {

    /** Values: PENDING,PROCESSING,COMPLETED,FAILED */
    enum class Status(@get:JsonValue val value: kotlin.String) {

        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        FAILED("FAILED");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Status {
                return values().first { it -> it.value == value }
            }
        }
    }
}
