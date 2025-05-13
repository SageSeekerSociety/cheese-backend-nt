package org.rucca.cheese.common.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "management.security.actuator")
@Validated
data class ActuatorCredentialsProperties(
    @field:NotBlank(message = "Actuator username must be configured") val username: String = "",
    @field:NotBlank(message = "Actuator password must be configured") val password: String = "",
    val role: String = "ENDPOINT_ADMIN",
)
