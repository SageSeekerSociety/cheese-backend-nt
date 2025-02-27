package org.rucca.cheese.llm.service

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrElse
import org.rucca.cheese.llm.config.LLMProperties
import org.rucca.cheese.llm.model.AIResourceType
import org.rucca.cheese.llm.model.UserAIQuota
import org.rucca.cheese.llm.model.UserSeuConsumption
import org.rucca.cheese.llm.repository.UserAIQuotaRepository
import org.rucca.cheese.llm.repository.UserSeuConsumptionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserQuotaService(
    private val userAIQuotaRepository: UserAIQuotaRepository,
    private val userSeuConsumptionRepository: UserSeuConsumptionRepository,
    private val seuCalculationService: SeuCalculationService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val properties: LLMProperties,
) {
    private val logger = LoggerFactory.getLogger(UserQuotaService::class.java)
    private val QUOTA_KEY_PREFIX = "user:seu:quota:"
    private val QUOTA_LOCK_PREFIX = "user:seu:quota:lock:"

    @Autowired private lateinit var applicationContext: ApplicationContext

    @Service
    class TransactionalService(
        private val userAIQuotaRepository: UserAIQuotaRepository,
        private val properties: LLMProperties,
    ) {
        @Transactional
        fun getOrCreateQuota(userId: Long): UserAIQuota {
            return try {
                userAIQuotaRepository.findByUserId(userId).orElseGet {
                    val quota =
                        UserAIQuota(
                            userId = userId,
                            dailySeuQuota = properties.quota.defaultDailyQuota.toBigDecimal(),
                            remainingSeu = properties.quota.defaultDailyQuota.toBigDecimal(),
                            totalSeuConsumed = BigDecimal.ZERO,
                            lastResetTime = LocalDateTime.now(),
                        )
                    userAIQuotaRepository.save(quota)
                    userAIQuotaRepository.findByUserId(userId).getOrElse {
                        throw QuotaUpdateError("Failed to get or create quota")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to get or create quota", e)
                throw QuotaUpdateError("Failed to get or create quota")
            }
        }

        @Transactional
        fun resetQuota(quota: UserAIQuota) {
            try {
                quota.remainingSeu = quota.dailySeuQuota
                quota.lastResetTime = LocalDateTime.now()
                userAIQuotaRepository.save(quota)
            } catch (e: Exception) {
                logger.error("Failed to reset quota", e)
                throw QuotaResetError("Failed to reset quota")
            }
        }

        @Transactional
        fun updateQuotaInDatabase(userId: Long, remainingSeu: BigDecimal) {
            try {
                val quota = getOrCreateQuota(userId)
                quota.remainingSeu = remainingSeu
                userAIQuotaRepository.save(quota)
            } catch (e: Exception) {
                logger.error("Failed to update quota in database", e)
                throw QuotaUpdateError("Failed to update quota in database")
            }
        }

        @Transactional
        fun resetAllQuotasInDatabase(quotas: List<UserAIQuota>) {
            quotas.forEach { quota ->
                try {
                    quota.remainingSeu = quota.dailySeuQuota
                    quota.lastResetTime = LocalDateTime.now()
                    userAIQuotaRepository.save(quota)
                } catch (e: Exception) {
                    logger.error("Failed to reset quota for user ${quota.userId}", e)
                }
            }
        }
    }

    fun checkAndDeductQuota(
        userId: Long,
        resourceType: AIResourceType,
        tokensUsed: Int,
        cacheKey: String? = null,
    ): UserSeuConsumption {
        // 检查缓存
        if (cacheKey != null) {
            userSeuConsumptionRepository
                .findByCacheKeyAndCacheExpireAtAfter(cacheKey, LocalDateTime.now())
                ?.let { cached ->
                    return recordConsumption(
                        userId = userId,
                        resourceType = resourceType,
                        tokensUsed = tokensUsed,
                        seuConsumed =
                            seuCalculationService.calculateSeuCost(
                                resourceType = resourceType,
                                tokensUsed = tokensUsed,
                                isCached = true,
                            ),
                        isCached = true,
                        cacheKey = cacheKey,
                        cacheExpireAt = cached.cacheExpireAt,
                    )
                }
        }

        // 计算SEU消耗
        val seuCost =
            seuCalculationService.calculateSeuCost(
                resourceType = resourceType,
                tokensUsed = tokensUsed,
            )

        val lockKey = "$QUOTA_LOCK_PREFIX$userId"
        val quotaKey = "$QUOTA_KEY_PREFIX$userId"
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)

        // 尝试获取分布式锁
        if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS) != true) {
            throw SystemBusyError("System is busy, please try again later")
        }

        try {
            // 检查 Redis 中的配额
            var remainingSeu = redisTemplate.opsForValue().get(quotaKey)?.toBigDecimalOrNull()

            if (remainingSeu == null) {
                // 如果 Redis 中没有配额信息，从数据库加载
                val quota = transactionalService.getOrCreateQuota(userId)
                checkAndResetQuota(quota)
                remainingSeu = quota.remainingSeu

                // 设置到 Redis，并设置过期时间到第二天的重置时间
                val expirationSeconds = calculateSecondsUntilNextReset()
                try {
                    redisTemplate
                        .opsForValue()
                        .set(quotaKey, remainingSeu.toString(), expirationSeconds, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    logger.error("Failed to set quota in Redis", e)
                    throw QuotaUpdateError("Failed to set quota in Redis")
                }
            }

            if (remainingSeu < seuCost) {
                throw QuotaExceededError("Insufficient SEU quota")
            }

            // 扣减配额
            val newRemainingSeu = remainingSeu.subtract(seuCost)
            try {
                redisTemplate.opsForValue().set(quotaKey, newRemainingSeu.toString())
            } catch (e: Exception) {
                logger.error("Failed to update quota in Redis", e)
                throw QuotaUpdateError("Failed to update quota in Redis")
            }

            // 异步更新数据库
            try {
                transactionalService.updateQuotaInDatabase(userId, newRemainingSeu)
            } catch (e: Exception) {
                logger.error("Failed to update quota in database", e)
                throw QuotaUpdateError("Failed to update quota in database")
            }

            // 记录消费
            return recordConsumption(
                userId = userId,
                resourceType = resourceType,
                tokensUsed = tokensUsed,
                seuConsumed = seuCost,
                isCached = false,
                cacheKey = cacheKey,
                cacheExpireAt = seuCalculationService.calculateCacheExpireTime(resourceType),
            )
        } finally {
            redisTemplate.delete(lockKey)
        }
    }

    fun getUserQuota(userId: Long): UserAIQuota {
        val quotaKey = "$QUOTA_KEY_PREFIX$userId"
        val transactionalService = applicationContext.getBean(TransactionalService::class.java)

        // 首先尝试从 Redis 获取
        val redisQuota = redisTemplate.opsForValue().get(quotaKey)?.toBigDecimalOrNull()
        if (redisQuota != null) {
            return transactionalService.getOrCreateQuota(userId).apply {
                this.remainingSeu = redisQuota
            }
        }

        // 如果 Redis 中没有，从数据库获取
        val quota = transactionalService.getOrCreateQuota(userId)
        checkAndResetQuota(quota)
        return quota
    }

    fun getUserResetTime(): OffsetDateTime {
        val nextReset = calculateNextResetTime()
        return OffsetDateTime.of(nextReset, OffsetDateTime.now().offset)
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
                        quota.remainingSeu.toString(),
                        expirationSeconds,
                        TimeUnit.SECONDS,
                    )
            } catch (e: Exception) {
                logger.error("Failed to reset quota", e)
                throw QuotaResetError("Failed to reset quota")
            }
        }
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
        logger.info("Starting to reset all users' SEU quotas")
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
                            quota.dailySeuQuota.toString(),
                            expirationSeconds,
                            TimeUnit.SECONDS,
                        )
                } catch (e: Exception) {
                    logger.error("Failed to reset Redis quota for user ${quota.userId}", e)
                }
            }

            logger.info("Successfully reset all users' SEU quotas")
        } catch (e: Exception) {
            logger.error("Failed to reset user SEU quotas", e)
            throw QuotaResetError("Failed to reset user SEU quotas")
        }
    }

    private fun recordConsumption(
        userId: Long,
        resourceType: AIResourceType,
        tokensUsed: Int,
        seuConsumed: BigDecimal,
        isCached: Boolean,
        cacheKey: String?,
        cacheExpireAt: LocalDateTime?,
    ): UserSeuConsumption {
        return userSeuConsumptionRepository.save(
            UserSeuConsumption(
                userId = userId,
                requestId = UUID.randomUUID().toString(),
                resourceType = resourceType,
                tokensUsed = tokensUsed,
                seuConsumed = seuConsumed,
                isCached = isCached,
                cacheKey = cacheKey,
                cacheExpireAt = cacheExpireAt,
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserQuotaService::class.java)
    }
}

class QuotaExceededError(message: String) : RuntimeException(message)

class QuotaUpdateError(message: String) : RuntimeException(message)

class QuotaResetError(message: String) : RuntimeException(message)

class SystemBusyError(message: String) : RuntimeException(message)
