package org.rucca.cheese.llm.service

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import org.rucca.cheese.llm.config.LLMProperties
import org.rucca.cheese.llm.model.AIResourceType
import org.springframework.stereotype.Service

@Service
class SeuCalculationService(private val llmProperties: LLMProperties) {
    fun calculateSeuCost(
        resourceType: AIResourceType,
        tokensUsed: Int,
        isCached: Boolean = false,
    ): BigDecimal {
        val baseCost =
            when (resourceType) {
                AIResourceType.LIGHTWEIGHT -> BigDecimal.ONE
                AIResourceType.STANDARD -> {
                    if (resourceType.dynamicCost) {
                        calculateDynamicCost(
                            tokensUsed,
                            llmProperties.quota.standardTokenRatio,
                        ).max(BigDecimal(resourceType.baseSeuCost))
                    } else {
                        BigDecimal(resourceType.baseSeuCost)
                    }
                }
                AIResourceType.ADVANCED ->
                    calculateDynamicCost(tokensUsed, llmProperties.quota.advancedTokenRatio)
            }

        return if (isCached) {
                baseCost.multiply(llmProperties.quota.cacheReuseRatio)
            } else {
                baseCost
            }
            .setScale(4, RoundingMode.HALF_UP)
    }

    private fun calculateDynamicCost(tokensUsed: Int, tokenRatio: BigDecimal): BigDecimal {
        return BigDecimal(tokensUsed)
            .multiply(tokenRatio)
            .divide(BigDecimal(1000), 4, RoundingMode.HALF_UP)
    }

    fun calculateCacheExpireTime(resourceType: AIResourceType): LocalDateTime? {
        return when (resourceType) {
            AIResourceType.LIGHTWEIGHT ->
                LocalDateTime.now().plusDays(llmProperties.quota.coldContentCacheDays.toLong())
            AIResourceType.STANDARD ->
                LocalDateTime.now().plusDays(llmProperties.quota.regularContentCacheDays.toLong())
            AIResourceType.ADVANCED -> null
        }
    }
}
