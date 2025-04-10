package org.rucca.cheese.user

import org.rucca.cheese.common.config.RedisPubSubConfig
import org.rucca.cheese.user.caches.UserInfoCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UserCacheUpdateListener(private val userInfoCache: UserInfoCache) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * This method is invoked by the MessageListenerAdapter when a message arrives at the
     * USER_UPDATE_CHANNEL. It expects the message body to be the userId whose cache needs eviction.
     *
     * @param message The message body received from Redis (expected to be the userId as String).
     * @param channel The channel the message came from (useful for logging).
     */
    fun handleMessage(message: String, channel: String) {
        log.info("Received message '{}' on channel '{}'", message, channel)
        if (channel == RedisPubSubConfig.USER_UPDATE_CHANNEL) {
            try {
                // Expecting the message to be the user ID
                val userId = message.toLong()
                // Call the cache component to evict the specific user's cache entry
                userInfoCache.evictUserDto(userId)
                log.info("Successfully processed cache eviction for user ID: {}", userId)
            } catch (e: NumberFormatException) {
                log.error("Received non-numeric user ID on channel '{}': {}", channel, message, e)
                // Decide how to handle invalid messages (e.g., log, discard)
            } catch (e: Exception) {
                log.error(
                    "Error processing message '{}' from channel '{}': {}",
                    message,
                    channel,
                    e.message,
                    e,
                )
                // Handle other potential errors during eviction
            }
        } else {
            log.warn("Received message on unexpected channel: {}", channel)
        }
    }
}
