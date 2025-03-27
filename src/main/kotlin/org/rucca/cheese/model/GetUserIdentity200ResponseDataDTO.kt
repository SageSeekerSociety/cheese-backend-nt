package org.rucca.cheese.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.Valid

/**
 * @param hasIdentity
 * @param identity
 */
data class GetUserIdentity200ResponseDataDTO(
    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("hasIdentity", required = true)
    val hasIdentity: kotlin.Boolean,
    @field:Valid
    @Schema(example = "null", description = "")
    @get:JsonProperty("identity")
    val identity: UserIdentityDTO? = null,
) : Serializable {

    companion object {
        private const val serialVersionUID: kotlin.Long = 1
    }
}
