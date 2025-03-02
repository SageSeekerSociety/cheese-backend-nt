package org.rucca.cheese.common.config

import java.time.Duration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): RedisCacheManager {
        val defaultCacheConfig =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 设置默认过期时间
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        StringRedisSerializer()
                    )
                )
                .disableCachingNullValues()
                .prefixCacheNameWith("cheese:caches:") // 设置缓存前缀

        // 为不同缓存配置不同的设置
        val cacheConfigurations = HashMap<String, RedisCacheConfiguration>()
        cacheConfigurations["topics"] = defaultCacheConfig.entryTtl(Duration.ofHours(1))

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
