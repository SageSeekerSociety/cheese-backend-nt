package org.rucca.cheese.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val applicationConfig: ApplicationConfig) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**") // Allow all paths
            .allowedOrigins(
                applicationConfig.corsOrigin
            ) // Allow the origin from the application config
            .allowedMethods("*") // Allow all methods
            .allowedHeaders("*") // Allow all headers
            .allowCredentials(true) // Allow credentials
            .maxAge(3600) // Set the max age to 1 hour
    }
}
