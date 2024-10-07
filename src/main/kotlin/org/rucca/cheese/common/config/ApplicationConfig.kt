package org.rucca.cheese.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "application")
class ApplicationConfig {
    lateinit var legacyUrl: String
    lateinit var jwtSecret: String
    lateinit var corsOrigin: String
    var shutdownOnStartup: Boolean = false
    var warnAuditFailure: Boolean = false
    var rankCheckEnforced: Boolean = false
    var rankJump: Int = 1
}
