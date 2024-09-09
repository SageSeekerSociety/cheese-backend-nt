package org.rucca.cheese.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "application")
class ApplicationConfig {
    lateinit var legacyUrl: String
}
