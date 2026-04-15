package org.rucca.cheese.common.config

import org.rucca.cheese.user.UserCacheUpdateListener // Import your listener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter

@Configuration
class RedisPubSubConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val USER_UPDATE_CHANNEL = "cache:user:updated"
    }

    /**
     * Configures the container that manages Redis message listeners. It needs the connection
     * factory to connect to Redis.
     *
     * @param connectionFactory Autoconfigured by Spring Boot based on application properties.
     * @param userUpdateListenerAdapter The adapter that links messages to our listener bean method.
     * @return The configured RedisMessageListenerContainer.
     */
    @Bean
    fun redisContainer(
        connectionFactory: RedisConnectionFactory,
        userUpdateListenerAdapter: MessageListenerAdapter,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(userUpdateListenerAdapter, ChannelTopic(USER_UPDATE_CHANNEL))
        log.info(
            "RedisMessageListenerContainer configured to listen on channel: {}",
            USER_UPDATE_CHANNEL,
        )
        return container
    }

    /**
     * Configures the adapter that forwards messages received on the channel to the specified method
     * ('handleMessage') on our listener bean ('userCacheUpdateListener'). This allows our listener
     * bean to be a simple POJO.
     *
     * @param listener The actual listener bean instance (UserCacheUpdateListener).
     * @return The configured MessageListenerAdapter.
     */
    @Bean
    fun userUpdateListenerAdapter(listener: UserCacheUpdateListener): MessageListenerAdapter {
        return MessageListenerAdapter(listener, "handleMessage")
    }
}
