package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/**
 * @param realName
 * @param studentId
 * @param grade
 * @param major
 * @param className
 */
data class UserIdentityDTO(
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
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
