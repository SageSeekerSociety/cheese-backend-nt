package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param type
 * @param projectIds
 * @param rootProjectId
 * @param includeRoot
 */
data class ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO(
    @Schema(example = "null", description = "")
    @get:JsonProperty("type")
    val type: ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO.Type? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("projectIds")
    val projectIds: kotlin.collections.List<kotlin.Long>? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("rootProjectId")
    val rootProjectId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("includeRoot")
    val includeRoot: kotlin.Boolean? = null,
) {

    /** Values: projects,tree */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        projects("projects"),
        tree("tree");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().first { it -> it.value == value }
            }
        }
    }
}
