package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param realName
 * @param studentId
 * @param grade
 * @param major
 * @param className
 * @param email
 * @param phone
 * @param applyReason
 * @param personalAdvantage
 * @param remark
 */
data class TaskParticipantRealNameInfoDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("realName", required = true)
    val realName: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("studentId", required = true)
    val studentId: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("grade", required = true)
    val grade: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("major", required = true)
    val major: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("className", required = true)
    val className: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("email", required = true)
    val email: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("phone", required = true)
    val phone: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applyReason", required = true)
    val applyReason: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personalAdvantage", required = true)
    val personalAdvantage: kotlin.String,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("remark", required = true)
    val remark: kotlin.String
) {}
