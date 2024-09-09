package org.rucca.cheese

import org.rucca.cheese.common.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication @EnableConfigurationProperties(ApplicationConfig::class) class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
