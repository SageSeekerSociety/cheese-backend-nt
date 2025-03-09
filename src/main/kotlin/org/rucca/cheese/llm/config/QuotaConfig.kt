package org.rucca.cheese.llm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QuotaConfig(private val properties: LLMProperties) {
    @Bean
    fun quotaResetCron(): String {
        // 生成 cron 表达式：每天在指定时间运行
        return "0 ${properties.quota.resetMinute} ${properties.quota.resetHour} * * *"
    }
}
