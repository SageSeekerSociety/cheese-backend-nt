package org.rucca.cheese.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val applicationConfig: ApplicationConfig) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
                .addMapping("/**") // 允许所有路径
                .allowedOrigins(applicationConfig.corsOrigin) // 允许所有源
                .allowedMethods("*") // 允许所有 HTTP 方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许发送 cookies
                .maxAge(3600) // 预检请求缓存 3600 秒
    }
}
