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
data class PutUserIdentityRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("realName")
    val realName: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("studentId")
    val studentId: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("grade")
    val grade: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("major")
    val major: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("className")
    val className: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
