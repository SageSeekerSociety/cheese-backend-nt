package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param name
 * @param description
 * @param type
 * @param content
 * @param teamId 知识条目所属的团队ID
 * @param projectId 相关的项目ID
 * @param labels
 */
data class UpdateKnowledgeRequestDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("name")
    val name: kotlin.String? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("description")
    val description: kotlin.String? = null,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("type")
    val type: KnowledgeTypeDTO? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("content")
    val content: kotlin.String? = null,
    @Schema(example = "null", description = "知识条目所属的团队ID")
    @get:JsonProperty("teamId")
    val teamId: kotlin.Long? = null,
    @Schema(example = "null", description = "相关的项目ID")
    @get:JsonProperty("projectId")
    val projectId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("labels")
    val labels: kotlin.collections.List<kotlin.String>? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
