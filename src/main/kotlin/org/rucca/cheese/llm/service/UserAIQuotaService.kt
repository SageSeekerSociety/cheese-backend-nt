package org.rucca.cheese.llm.service

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import org.rucca.cheese.llm.config.LLMProperties
import org.rucca.cheese.llm.error.LLMError
import org.rucca.cheese.llm.model.UserAIQuota
import org.rucca.cheese.llm.repository.UserAIQuotaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAIQuotaService(
    private val userAIQuotaRepository: UserAIQuotaRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val properties: LLMProperties,
) {
    private val logger = LoggerFactory.getLogger(UserAIQuotaService::class.java)
    private val QUOTA_KEY_PREFIX = "user:ai:quota:"
    private val QUOTA_LOCK_PREFIX = "user:ai:quota:lock:"

    @Autowired private lateinit var applicationContext: ApplicationContext

    @Service
    class TransactionalService(
        private val userAIQuotaRepository: UserAIQuotaRepository,
        private val properties: LLMProperties,
    ) {
        @Transactional
        fun getOrCreateQuota(userId: Long): UserAIQuota {
            return try {
                userAIQuotaRepository.findById(userId).orElseGet {
                    UserAIQuota(
                            userId = userId,
                            dailyQuota = properties.quota.defaultDailyQuota,
                            remainingQuota = properties.quota.defaultDailyQuota,
                            lastResetTime = LocalDateTime.now(),
                        )
                        .also { userAIQuotaRepository.save(it) }
                }
            } catch (e: Exception) {
                logger.error("Failed to get or create quota", e)
                throw LLMError.QuotaUpdateError()
            }
        }

        @Transactional
        fun resetQuota(quota: UserAIQuota) {
            try {
                quota.remainingQuota = quota.dailyQuota
                quota.lastResetTime = LocalDateTime.now()
                userAIQuotaRepository.save(quota)
            } catch (e: Exception) {
                logger.error("Failed to reset quota", e)
                throw LLMError.QuotaResetError()
            }
        }

        @Transactional
        fun updateQuotaInDatabase(userId: Long, remainingQuota: Int) {
            try {
                val quota = getOrCreateQuota(userId)
                quota.remainingQuota = remainingQuota
                quota.updatedAt = LocalDateTime.now()
                userAIQuotaRepository.save(quota)
            } catch (e: Exception) {
                logger.error("Failed to update quota in database", e)
                throw LLMError.QuotaUpdateError()
            }
        }

        @Transactional
        fun resetAllQuotasInDatabase(quotas: List<UserAIQuota>) {
            quotas.forEach { quota ->
                try {
                    quota.remainingQuota = quota.dailyQuota
                    quota.lastResetTime = LocalDateTime.now()
                    userAIQuotaRepository.save(quota)
                } catch (e: Exception) {
                    logger.error("Failed to reset quota for user ${quota.userId}", e)
                }
            }
        }
    }

    fun checkAndDeductQuota(userId: Long): Boolean {
        val lockKey = "$QUOTA_LOCK_PREFIX$userId"
        val quotaKey = "$QUOTA_KEY_PREFIX$userId"
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)

        // 尝试获取分布式锁
        if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS) != true) {
            throw LLMError.SystemBusyError()
        }

        try {
            // 检查 Redis 中的配额
            var remainingQuota = redisTemplate.opsForValue().get(quotaKey)?.toIntOrNull()

            if (remainingQuota == null) {
                // 如果 Redis 中没有配额信息，从数据库加载
                val quota = transactionalService.getOrCreateQuota(userId)
                checkAndResetQuota(quota)
                remainingQuota = quota.remainingQuota

                // 设置到 Redis，并设置过期时间到第二天的重置时间
                val expirationSeconds = calculateSecondsUntilNextReset()
                try {
                    redisTemplate
                        .opsForValue()
                        .set(
                            quotaKey,
                            remainingQuota.toString(),
                            expirationSeconds,
                            TimeUnit.SECONDS,
                        )
                } catch (e: Exception) {
                    logger.error("Failed to set quota in Redis", e)
                    throw LLMError.QuotaUpdateError()
                }
            }

            if (remainingQuota <= 0) {
                throw LLMError.QuotaExceededError()
            }

            // 扣减配额
            try {
                redisTemplate.opsForValue().decrement(quotaKey)
            } catch (e: Exception) {
                logger.error("Failed to decrement quota in Redis", e)
                throw LLMError.QuotaUpdateError()
            }

            // 异步更新数据库
            try {
                transactionalService.updateQuotaInDatabase(userId, remainingQuota - 1)
            } catch (e: Exception) {
                logger.error("Failed to update quota in database", e)
                throw LLMError.QuotaUpdateError()
            }

            return true
        } finally {
            redisTemplate.delete(lockKey)
        }
    }

    private fun checkAndResetQuota(quota: UserAIQuota) {
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)
        val now = LocalDateTime.now()
        val lastResetDate = quota.lastResetTime.toLocalDate()
        val today = now.toLocalDate()

        if (lastResetDate.isBefore(today)) {
            try {
                transactionalService.resetQuota(quota)

                // 同步更新 Redis
                val quotaKey = "$QUOTA_KEY_PREFIX${quota.userId}"
                val expirationSeconds = calculateSecondsUntilNextReset()
                redisTemplate
                    .opsForValue()
                    .set(
                        quotaKey,
                        quota.remainingQuota.toString(),
                        expirationSeconds,
                        TimeUnit.SECONDS,
                    )
            } catch (e: Exception) {
                logger.error("Failed to reset quota", e)
                throw LLMError.QuotaResetError()
            }
        }
    }

    fun getUserQuota(userId: Long): UserAIQuota {
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)
        return transactionalService.getOrCreateQuota(userId).also { checkAndResetQuota(it) }
    }

    fun getUserResetTime(): OffsetDateTime {
        val nextReset = calculateNextResetTime()
        return OffsetDateTime.of(nextReset, OffsetDateTime.now().offset)
    }

    fun getRemainingQuota(userId: Long): Int {
        val quotaKey = "$QUOTA_KEY_PREFIX$userId"
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)

        // 首先尝试从 Redis 获取
        try {
            val redisQuota = redisTemplate.opsForValue().get(quotaKey)?.toIntOrNull()
            if (redisQuota != null) {
                return redisQuota
            }
        } catch (e: Exception) {
            logger.warn("Failed to get quota from Redis", e)
            // 继续尝试从数据库获取
        }

        // 如果 Redis 中没有，从数据库获取
        val quota = transactionalService.getOrCreateQuota(userId)
        checkAndResetQuota(quota)
        return quota.remainingQuota
    }

    private fun calculateNextResetTime(): LocalDateTime {
        val now = LocalDateTime.now()
        val resetTime = LocalTime.of(properties.quota.resetHour, properties.quota.resetMinute)
        var nextReset = now.toLocalDate().atTime(resetTime)

        if (now.toLocalTime().isAfter(resetTime)) {
            nextReset = nextReset.plusDays(1)
        }

        return nextReset
    }

    private fun calculateSecondsUntilNextReset(): Long {
        val now = LocalDateTime.now()
        val nextReset = calculateNextResetTime()
        return java.time.Duration.between(now, nextReset).seconds
    }

    // 每天在重置时间运行，重置所有用户的配额
    @Scheduled(cron = "#{@quotaResetCron}")
    fun resetAllQuotas() {
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)
        logger.info("Starting to reset all users' AI quotas")
        try {
            // 获取所有用户的配额记录
            val quotas = userAIQuotaRepository.findAll()

            // 使用事务服务重置数据库中的配额
            transactionalService.resetAllQuotasInDatabase(quotas)

            // 重置 Redis 中的配额
            quotas.forEach { quota ->
                try {
                    val quotaKey = "$QUOTA_KEY_PREFIX${quota.userId}"
                    val expirationSeconds = calculateSecondsUntilNextReset()
                    redisTemplate
                        .opsForValue()
                        .set(
                            quotaKey,
                            quota.dailyQuota.toString(),
                            expirationSeconds,
                            TimeUnit.SECONDS,
                        )
                } catch (e: Exception) {
                    logger.error("Failed to reset Redis quota for user ${quota.userId}", e)
                }
            }

            logger.info("Successfully reset all users' AI quotas")
        } catch (e: Exception) {
            logger.error("Failed to reset user AI quotas", e)
            throw LLMError.QuotaResetError()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserAIQuotaService::class.java)
    }
}
