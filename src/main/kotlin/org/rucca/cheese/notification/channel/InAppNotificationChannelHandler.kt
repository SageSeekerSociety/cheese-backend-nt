package org.rucca.cheese.notification.channel

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.notification.models.Notification
import org.rucca.cheese.notification.models.NotificationChannel
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.notification.repositories.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class InAppNotificationChannelHandler(
    private val notificationRepository: NotificationRepository,
    private val objectMapper: ObjectMapper,
) : NotificationChannelHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supportedChannel(): NotificationChannel = NotificationChannel.IN_APP

    override suspend fun sendNotification(
        recipientId: Long,
        type: NotificationType,
        payload: Map<String, Any>,
        recipientLocale: Locale,
        isAggregatedFinalization: Boolean,
        renderedTitle: String?,
        renderedBody: String?,
    ) {
        // IN_APP Handler Responsibility:
        // - Create the Notification record for NEW, NON-AGGREGATED notifications.
        // - Do nothing (or trigger push) for AGGREGATED FINALIZATION (record created by
        // EventHandler).
        // - Do nothing for initial AGGREGATION events (placeholder created by EventHandler).

        if (!isAggregatedFinalization) {
            // This is a NEW, non-aggregated notification (or initial aggregation placeholder)
            withContext(Dispatchers.IO) {
                try {
                    val metadataJson = objectMapper.writeValueAsString(payload)

                    // Create and save the entity
                    // Note: Aggregation logic in NotificationService handles creation of
                    // placeholders
                    // This path should only be hit for non-aggregatable types now.
                    val notification =
                        Notification(
                            type = type,
                            receiverId = recipientId,
                            metadata = metadataJson,
                            read = false,
                            finalized = true,
                            isAggregatable = false,
                        )
                    notificationRepository.save(notification)
                    log.debug(
                        "Saved IN_APP notification for recipient {}: {}",
                        recipientId,
                        notification.id,
                    )
                } catch (e: Exception) {
                    log.error(
                        "Failed to save IN_APP notification for recipient {}: {}",
                        recipientId,
                        e.message,
                        e,
                    )
                }
            }
        } else {
            log.debug(
                "IN_APP notification for recipient {} (type {}) was finalized by job.",
                recipientId,
                type,
            )
        }
    }
}
