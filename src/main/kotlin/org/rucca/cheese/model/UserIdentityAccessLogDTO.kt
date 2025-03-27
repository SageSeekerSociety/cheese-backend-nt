package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param accessor
 * @param accessTime
 * @param accessType
 * @param ipAddress
 * @param accessModuleType
 * @param accessEntityId
 * @param accessEntityName
 */
data class UserIdentityAccessLogDTO(
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("accessor", required = true)
    val accessor: UserDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("accessTime", required = true)
    val accessTime: kotlin.Long,
    @field:Valid
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("accessType", required = true)
    val accessType: UserIdentityAccessTypeDTO,
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("ipAddress", required = true)
    val ipAddress: kotlin.String,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("accessModuleType")
    val accessModuleType: UserIdentityAccessModuleTypeDTO? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("accessEntityId")
    val accessEntityId: kotlin.Long? = null,
    @Schema(example = "null", description = "")
    @get:JsonProperty("accessEntityName")
    val accessEntityName: kotlin.String? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
