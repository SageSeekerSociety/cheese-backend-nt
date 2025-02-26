package org.rucca.cheese.llm.config

import java.math.BigDecimal
import org.rucca.cheese.llm.error.LLMError.ModelNotFoundError
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cheese.llm")
data class LLMProperties(
    val apiKey: String = "",
    val baseUrl: String = "",
    val models: Map<String, ModelProperties> = mapOf(),
    val defaultModelType: String = "standard",
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
        val standardTokenRatio: BigDecimal = BigDecimal("0.5"),
        val advancedTokenRatio: BigDecimal = BigDecimal("1.0"),
        val cacheReuseRatio: BigDecimal = BigDecimal("0.2"),
        val coldContentCacheDays: Int = 365,
        val regularContentCacheDays: Int = 30,
    )

    data class TimeoutProperties(
        val socket: Long = 60,
        val connect: Long = 60,
        val request: Long = 600,
    )

    val defaultModel: ModelProperties
        get() = models[defaultModelType] ?: throw ModelNotFoundError(defaultModelType)

    /**
     * 获取指定模型类型的配置
     *
     * @param modelType 模型类型标识符
     * @return 模型配置
     * @throws ModelNotFoundError 如果指定的模型类型不存在
     */
    fun getModelConfig(modelType: String? = null): ModelProperties {
        // 如果未指定模型类型，返回默认模型配置
        if (modelType == null) {
            return defaultModel
        }

        // 如果指定了模型类型，检查是否存在
        return models[modelType] ?: throw ModelNotFoundError(modelType)
    }
}
