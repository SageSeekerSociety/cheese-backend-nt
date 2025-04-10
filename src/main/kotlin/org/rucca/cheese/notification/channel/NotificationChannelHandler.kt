package org.rucca.cheese.notification.channel

import java.util.Locale
import org.rucca.cheese.notification.models.NotificationChannel
import org.rucca.cheese.notification.models.NotificationType

interface NotificationChannelHandler {
    fun supportedChannel(): NotificationChannel

    /**
     * Send notification via this channel. Implementations handle template rendering (using
     * recipientLocale) and dispatching. Should ideally be suspending or delegate to async
     * execution.
     *
     * @param recipientId The target user's ID.
     * @param type The notification type.
     * @param payload The context data.
     * @param recipientLocale The preferred locale of the recipient for i18n.
     * @param isAggregatedFinalization Flag indicating if this dispatch is due to an aggregation job
     *   finalizing.
     * @param renderedTitle Dynamically rendered title (populated for external channels).
     * @param renderedBody Dynamically rendered body (populated for external channels).
     */
    suspend fun sendNotification(
        recipientId: Long,
        type: NotificationType,
        payload: Map<String, Any>,
        recipientLocale: Locale,
        isAggregatedFinalization: Boolean,
        renderedTitle: String?,
        renderedBody: String?,
    )
}
