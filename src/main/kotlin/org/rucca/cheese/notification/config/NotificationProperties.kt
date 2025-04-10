package org.rucca.cheese.notification.config

import java.time.Duration
import org.rucca.cheese.notification.models.NotificationChannel
import org.rucca.cheese.notification.models.NotificationType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cheese.notification")
data class NotificationProperties(
    @DefaultValue("IN_APP")
    var defaultActiveChannels: List<NotificationChannel> = listOf(NotificationChannel.IN_APP),
    var typeChannels: Map<NotificationType, List<NotificationChannel>> = emptyMap(),

    // --- Aggregation Settings ---
    var aggregatableTypes: Set<NotificationType> = emptySet(), // e.g., [REACTION]
    @DefaultValue("PT5M") // Default 5 minutes
    var aggregationWindow: Duration = Duration.ofMinutes(5),
    @DefaultValue("3") var aggregationMaxActorsInTitle: Int = 3,
) {
    fun getActiveChannelsForType(type: NotificationType): List<NotificationChannel> {
        return typeChannels[type] ?: defaultActiveChannels
    }

    fun isAggregatable(type: NotificationType): Boolean {
        return aggregatableTypes.contains(type)
    }
}
