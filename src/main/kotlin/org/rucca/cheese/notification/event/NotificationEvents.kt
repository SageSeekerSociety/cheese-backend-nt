package org.rucca.cheese.notification.event

import org.rucca.cheese.notification.models.NotificationType

/** Event published by business modules to trigger a notification. */
data class NotificationTriggerEvent(
    val source: Any, // The object publishing the event
    val recipientIds: Set<Long>, // User IDs
    val type: NotificationType,
    val payload:
        Map<String, Any>, // Data for templates and metadata (e.g., actorId, postId, commentId)
    val actorId: Long? = null, // Optional convenience: ID of the user performing the action
)
