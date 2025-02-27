package org.rucca.cheese.llm.config

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import kotlin.time.Duration.Companion.seconds
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(LLMProperties::class)
class LLMConfig(private val properties: LLMProperties) {
    @Bean
    fun llmClient(): OpenAI {
        return OpenAI(
            OpenAIConfig(
                token = properties.apiKey,
                host = OpenAIHost(baseUrl = properties.baseUrl),
                timeout =
                    Timeout(
                        socket = properties.timeout.socket.seconds,
                        connect = properties.timeout.connect.seconds,
                        request = properties.timeout.request.seconds,
                    ),
            )
        )
    }
}
