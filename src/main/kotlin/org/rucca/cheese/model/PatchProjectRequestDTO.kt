package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import java.io.Serializable

/**
 * @param name
 * @param description
 * @param colorCode
 * @param startDate 项目开始时间戳(毫秒)
 * @param endDate 项目结束时间戳(毫秒)
 * @param githubRepo
 * @param archived 是否归档
 */
data class PatchProjectRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @get:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Schema(example = "null", description = "")
    @get:JsonProperty("colorCode")
    val colorCode: kotlin.String? = null,
    @Schema(example = "null", description = "项目开始时间戳(毫秒)")
    @get:JsonProperty("startDate")
    val startDate: kotlin.Long? = null,
    @Schema(example = "null", description = "项目结束时间戳(毫秒)")
    @get:JsonProperty("endDate")
    val endDate: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("githubRepo")
    val githubRepo: kotlin.String? = null,
    @Schema(example = "null", description = "是否归档")
    @get:JsonProperty("archived")
    val archived: kotlin.Boolean? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
