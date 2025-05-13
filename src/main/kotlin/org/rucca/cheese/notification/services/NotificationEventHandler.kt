package org.rucca.cheese.notification.services

// proxy
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Provider // Keep Provider for self-injection if needed for @Transactional
import jakarta.persistence.OptimisticLockException
import java.time.Instant
import java.util.*
import kotlinx.coroutines.*
import org.rucca.cheese.notification.channel.NotificationChannelHandler
import org.rucca.cheese.notification.config.NotificationProperties
import org.rucca.cheese.notification.event.NotificationTriggerEvent
import org.rucca.cheese.notification.models.Notification
import org.rucca.cheese.notification.models.NotificationChannel
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.notification.repositories.NotificationRepository
import org.rucca.cheese.notification.resolver.NotificationEntityResolverFacade
import org.rucca.cheese.user.services.UserService // Keep for locale
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class NotificationEventHandler(
    private val channelHandlers: List<NotificationChannelHandler>,
    private val notificationProperties: NotificationProperties,
    private val notificationRepository: NotificationRepository,
    private val externalNotificationRenderer: ExternalNotificationRenderer,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
    private val selfProvider: Provider<NotificationEventHandler>,
) {
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val log = LoggerFactory.getLogger(javaClass)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val notificationDispatcher = Dispatchers.IO.limitedParallelism(10)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleNotificationTrigger(event: NotificationTriggerEvent) {
        log.debug("Handling event: type={}, recipients={}", event.type, event.recipientIds.size)

        if (notificationProperties.isAggregatable(event.type)) {
            handleAggregatableEvent(event)
        } else {
            handleRegularEvent(event)
        }
    }

    private fun handleRegularEvent(event: NotificationTriggerEvent) {
        applicationScope.launch(
            notificationDispatcher + CoroutineName("notif-regular-${event.type}")
        ) {
            dispatchToHandlers(
                event.recipientIds,
                event.type,
                event.payload,
                isAggregatedFinalization = false,
            )
        }
    }

    private fun handleAggregatableEvent(event: NotificationTriggerEvent) {
        event.recipientIds.forEach { recipientId ->
            applicationScope.launch(
                notificationDispatcher + CoroutineName("notif-agg-${event.type}-R$recipientId")
            ) {
                try {
                    selfProvider
                        .get()
                        .processAggregationOrStartNew(recipientId, event.type, event.payload)
                } catch (e: OptimisticLockException) {
                    log.warn(
                        "Optimistic lock failed during aggregation for recipient={}, type={}. Msg: {}",
                        recipientId,
                        event.type,
                        e.message,
                    )
                } catch (e: Exception) {
                    log.error(
                        "Error processing aggregatable event for recipient={}, type={}: {}",
                        recipientId,
                        event.type,
                        e.message,
                        e,
                    )
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    suspend fun processAggregationOrStartNew(
        recipientId: Long,
        type: NotificationType,
        payload: Map<String, Any>,
    ) {
        val aggregationKey = generateAggregationKey(type, payload)
        if (aggregationKey == null && notificationProperties.isAggregatable(type)) {
            log.warn(
                "Cannot generate aggregation key for aggregatable type {} / payload. Sending as regular.",
                type,
            )
            applicationScope.launch(
                notificationDispatcher + CoroutineName("notif-fallback-${type}-R$recipientId")
            ) {
                dispatchToHandlers(setOf(recipientId), type, payload, false)
            }
            return
        }

        val existingNotification =
            withContext(Dispatchers.IO) {
                notificationRepository.findActiveAggregation(
                    recipientId,
                    aggregationKey!!,
                    Instant.now(),
                )
            }

        if (existingNotification != null) {
            val updatedMetadata = mergeMetadata(existingNotification.metadata, payload, type)
            existingNotification.metadata = objectMapper.writeValueAsString(updatedMetadata)
            withContext(Dispatchers.IO) { notificationRepository.save(existingNotification) }
            log.debug("Aggregated event into notification ID: {}", existingNotification.id)
        } else {
            val aggregateUntil = Instant.now().plus(notificationProperties.aggregationWindow)
            val initialMetadata = createInitialMetadata(payload, type)
            val metadataJson = objectMapper.writeValueAsString(initialMetadata)

            val newNotification =
                Notification(
                    type = type,
                    receiverId = recipientId,
                    metadata = metadataJson,
                    read = false,
                    isAggregatable = true,
                    aggregationKey = aggregationKey,
                    aggregateUntil = aggregateUntil,
                    finalized = false,
                )
            withContext(Dispatchers.IO) { notificationRepository.save(newNotification) }
            log.debug(
                "Started new aggregation placeholder notification ID: {}, Key: {}",
                newNotification.id,
                aggregationKey,
            )
        }
    }

    suspend fun dispatchToHandlers(
        recipientIds: Set<Long>,
        type: NotificationType,
        payload: Map<String, Any>,
        isAggregatedFinalization: Boolean,
    ) {
        val activeChannels = notificationProperties.getActiveChannelsForType(type)
        if (activeChannels.isEmpty()) return
        val applicableHandlers =
            channelHandlers.filter { activeChannels.contains(it.supportedChannel()) }
        if (applicableHandlers.isEmpty()) return

        recipientIds.forEach { recipientId ->
            val recipientLocale =
                try {
                    userService.getUserLocale(recipientId)
                } catch (e: Exception) {
                    Locale.getDefault()
                }

            applicableHandlers.forEach { handler ->
                val shouldDispatch =
                    when {
                        // IN_APP handler decides internally based on state (creation vs
                        // finalization)
                        handler.supportedChannel() == NotificationChannel.IN_APP -> true
                        // External channels dispatch only if NOT initial aggregation OR IS
                        // finalization
                        !notificationProperties.isAggregatable(type) || isAggregatedFinalization ->
                            true
                        else -> false
                    }

                if (shouldDispatch) {
                    applicationScope.launch(
                        notificationDispatcher +
                            CoroutineName("dispatch-${handler.supportedChannel()}-R$recipientId")
                    ) {
                        try {
                            // External channels need dynamic rendering now
                            var titleToUse: String? = null
                            var bodyToUse: String? = null
                            if (handler.supportedChannel() != NotificationChannel.IN_APP) {
                                // Use the new renderer service for external channels
                                titleToUse =
                                    externalNotificationRenderer.renderTitle(
                                        type,
                                        payload,
                                        recipientLocale,
                                        isAggregatedFinalization,
                                    )
                                bodyToUse =
                                    externalNotificationRenderer.renderBody(
                                        type,
                                        payload,
                                        recipientLocale,
                                        isAggregatedFinalization,
                                        handler.supportedChannel(),
                                    ) // Pass channel for potential format diffs
                            }

                            // Pass dynamically rendered content (or null for IN_APP) to handler
                            handler.sendNotification(
                                recipientId,
                                type,
                                payload,
                                recipientLocale,
                                isAggregatedFinalization,
                                titleToUse,
                                bodyToUse,
                            )
                        } catch (e: Exception) {
                            log.error(
                                "Handler {} failed for recipient {}: {}",
                                handler.supportedChannel(),
                                recipientId,
                                e.message,
                                e,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generateAggregationKey(type: NotificationType, payload: Map<String, Any>): String? {
        // Example for Reaction, assuming payload contains:
        // "targetItem": {"type": "discussionComment", "id": "567"}
        return when (type) {
            NotificationType.REACTION -> {
                val targetItem = payload["targetItem"] as? Map<*, *>
                val targetType = targetItem?.get("type") as? String
                val targetId = targetItem?.get("id") as? String
                if (targetType != null && targetId != null) {
                    "$type:$targetType:$targetId"
                } else null
            }
            // Add other types...
            else -> null
        }
    }

    private fun createInitialMetadata(
        payload: Map<String, Any>,
        type: NotificationType,
    ): Map<String, Any> {
        // Keep entity references and context data
        val metadata = payload.toMutableMap() // Start with the original payload

        if (type == NotificationType.REACTION) {
            val actorRef = payload["actor"] as? Map<*, *>
            val actorId = actorRef?.get("id") as? String
            // Store initial reactor info (maybe just ID is enough if resolved later)
            metadata["reactorIds"] =
                if (actorId != null) mutableListOf(actorId) else mutableListOf<String>()
            // metadata["reactorUsernames"] = ... // No longer store usernames here
            metadata["totalCount"] = 1
        }
        // Add other type-specific initial metadata state if needed
        return metadata
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergeMetadata(
        existingMetadataJson: String?,
        newPayload: Map<String, Any>,
        type: NotificationType,
    ): Map<String, Any> {
        val currentMetadata =
            try {
                existingMetadataJson?.let { objectMapper.readValue<MutableMap<String, Any>>(it) }
                    ?: mutableMapOf()
            } catch (e: Exception) {
                log.warn("Failed parsing existing metadata: {}", e.message)
                mutableMapOf()
            }

        if (type == NotificationType.REACTION) {
            val newActorRef = newPayload["actor"] as? Map<*, *>
            val newReactorId = newActorRef?.get("id") as? String
            val currentReactorIds =
                (currentMetadata.getOrPut("reactorIds") { mutableListOf<String>() }
                    as MutableList<String>)

            if (newReactorId != null && !currentReactorIds.contains(newReactorId)) {
                currentReactorIds.add(newReactorId)
                currentMetadata["totalCount"] =
                    (currentMetadata["totalCount"] as? Number ?: 0).toInt() + 1
                // Remove outdated reactorUsernames if present from old logic
                currentMetadata.remove("reactorUsernames")
            }
        }
        // Add merge logic for other types...

        // Ensure essential context is preserved (e.g., targetItem ref from newPayload if missing)
        newPayload.forEach { (key, value) -> currentMetadata.putIfAbsent(key, value) }

        return currentMetadata
    }

    // REMOVED: getUsername helper from here (belongs in resolver/UserService)
}

/**
 * New Service responsible for rendering notification content dynamically, especially for external
 * channels that don't persist the rendered state. It uses the NotificationTemplateService
 * internally.
 */
@Service
class ExternalNotificationRenderer(
    private val templateService: NotificationTemplateService, // The service with i18n logic
    private val entityResolverFacade: NotificationEntityResolverFacade, // To resolve IDs
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun renderTitle(
        type: NotificationType,
        payload: Map<String, Any>,
        locale: Locale,
        isAggregated: Boolean,
    ): String {
        return try {
            if (isAggregated) {
                // For aggregated, payload is the aggregated metadata
                templateService.renderAggregatedTitle(type, payload, locale)
            } else {
                // For regular, resolve entities within the payload first
                val resolvedPayload = resolvePayloadEntities(payload)
                templateService.renderTitle(
                    type,
                    NotificationChannel.EMAIL,
                    resolvedPayload,
                    locale,
                ) // Use EMAIL channel context?
            }
        } catch (e: Exception) {
            log.error("Error rendering title for type {}: {}", type, e.message, e)
            "Notification" // Fallback title
        }
    }

    suspend fun renderBody(
        type: NotificationType,
        payload: Map<String, Any>,
        locale: Locale,
        isAggregated: Boolean,
        channel: NotificationChannel,
    ): String {
        return try {
            if (isAggregated) {
                templateService.renderAggregatedBody(type, payload, locale)
            } else {
                val resolvedPayload = resolvePayloadEntities(payload)
                templateService.renderBody(type, channel, resolvedPayload, locale)
            }
        } catch (e: Exception) {
            log.error("Error rendering body for type {}: {}", type, e.message, e)
            "You have a new notification." // Fallback body
        }
    }

    /**
     * Helper to resolve entity references within a single notification's payload. Returns a new map
     * where entity references are replaced with resolved info.
     */
    private suspend fun resolvePayloadEntities(payload: Map<String, Any>): Map<String, Any> {
        // Resolve entities for this single payload
        val resolvedEntities = entityResolverFacade.resolveEntitiesFromMetadata(listOf(payload))
        val enrichedPayload = payload.toMutableMap()

        // Replace entity references in the payload with resolved info (or keep original if needed)
        payload.forEach { (key, value) ->
            if (value is Map<*, *>) {
                val entityType = value["type"] as? String
                val entityId = value["id"] as? String
                if (entityType != null && entityId != null) {
                    val resolvedInfo = resolvedEntities[entityType]?.get(entityId)
                    if (resolvedInfo != null) {
                        // Replace the reference map with the resolved info map
                        enrichedPayload[key] = resolvedInfo // Store the ResolvableEntityInfo object
                    } else {
                        log.warn(
                            "Could not resolve entity in payload: type={}, id={}",
                            entityType,
                            entityId,
                        )
                        // Keep original reference or replace with placeholder? Keep for now.
                    }
                }
            }
        }
        return enrichedPayload
    }
}
