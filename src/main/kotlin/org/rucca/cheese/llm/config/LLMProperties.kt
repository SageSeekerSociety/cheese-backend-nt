package org.rucca.cheese.llm.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cheese.llm")
data class LLMProperties(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: ModelProperties = ModelProperties(),
    val quota: QuotaProperties = QuotaProperties(),
    val timeout: TimeoutProperties = TimeoutProperties(),
) {
    data class ModelProperties(
        val name: String = "gpt-3.5-turbo",
        val temperature: Double = 0.7,
        val maxTokens: Int = 2048,
        val topP: Double = 1.0,
        val frequencyPenalty: Double = 0.0,
        val presencePenalty: Double = 0.0,
    )

    data class QuotaProperties(
        val defaultDailyQuota: Int = 10,
        val resetHour: Int = 0,
        val resetMinute: Int = 0,
    )

    data class TimeoutProperties(
        val socket: Long = 60,
        val connect: Long = 60,
        val request: Long = 600,
    )
}
