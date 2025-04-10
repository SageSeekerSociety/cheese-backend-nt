package org.rucca.cheese.notification.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.OptimisticLockException
import java.time.Instant
import kotlinx.coroutines.*
import org.rucca.cheese.notification.models.Notification
import org.rucca.cheese.notification.repositories.NotificationRepository
import org.rucca.cheese.notification.services.NotificationEventHandler
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationAggregationJob(
    private val notificationRepository: NotificationRepository,
    private val notificationEventHandler: NotificationEventHandler,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val jobScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional(propagation = Propagation.NEVER)
    fun finalizeAggregatedNotifications() {
        log.debug("Running notification aggregation finalization job.")
        val now = Instant.now()
        // Use withContext for DB operation if repository isn't suspending
        val expiredNotifications =
            runBlocking(Dispatchers.IO) { // Or use jobScope if repo is suspending
                notificationRepository.findExpiredAggregations(now)
            }

        if (expiredNotifications.isEmpty()) {
            /* ... log debug ... */
            return
        }
        log.info("Found {} notifications to finalize.", expiredNotifications.size)

        expiredNotifications.forEach { notification ->
            jobScope.launch(CoroutineName("finalize-${notification.id}")) {
                try {
                    // Use withContext for transactional method if it's not suspending
                    runBlocking(Dispatchers.IO) { // Or call directly if suspending
                        finalizeAndDispatch(notification)
                    }
                } catch (e: OptimisticLockException) {
                    /* ... log warn ... */
                } catch (e: Exception) {
                    /* ... log error ... */
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // Consider making suspend if DB operations within are suspending
    suspend fun finalizeAndDispatch(notification: Notification) {
        // Re-fetch within transaction
        val freshNotification =
            withContext(Dispatchers.IO) { notificationRepository.findById(notification.id!!) }
                .orElse(null)

        if (freshNotification == null || freshNotification.finalized /* ... other checks ... */) {
            log.warn(
                "Notification ID {} no longer requires finalization. Skipping.",
                notification.id,
            )
            return
        }

        val metadata =
            try {
                objectMapper.readValue<Map<String, Any>>(freshNotification.metadata ?: "{}")
            } catch (e: Exception) {
                mapOf()
            }

        freshNotification.finalized = true
        freshNotification.isAggregatable = false

        val savedNotification =
            withContext(Dispatchers.IO) { notificationRepository.save(freshNotification) }
        log.info("Finalized aggregation state for notification ID: {}", savedNotification.id)

        notificationEventHandler.dispatchToHandlers(
            recipientIds = setOf(savedNotification.receiverId),
            type = savedNotification.type,
            payload = metadata,
            isAggregatedFinalization = true,
        )
    }
}
