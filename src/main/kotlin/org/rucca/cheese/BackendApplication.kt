package org.rucca.cheese

import org.rucca.cheese.common.config.ApplicationConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig::class)
class BackendApplication(private val applicationConfig: ApplicationConfig) {
    @Bean
    fun applicationReadyListener(): ApplicationListener<ApplicationReadyEvent> {
        return ApplicationListener { event ->
            if (applicationConfig.shutdownOnStartup) {
                LoggerFactory.getLogger(BackendApplication::class.java)
                        .info("Shutting down application as requested by configuration")
                SpringApplication.exit(event.applicationContext)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
