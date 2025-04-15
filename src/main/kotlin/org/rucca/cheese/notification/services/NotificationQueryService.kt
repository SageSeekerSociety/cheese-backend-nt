package org.rucca.cheese.notification.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.spec.CursorSpecificationBuilder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.EncodedCursorPageDTO
import org.rucca.cheese.model.NotificationDTO
import org.rucca.cheese.notification.models.Notification
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.notification.models.toDTO
import org.rucca.cheese.notification.repositories.NotificationRepository
import org.rucca.cheese.notification.resolver.NotificationEntityResolverFacade
import org.rucca.cheese.notification.resolver.ResolvableEntityInfo
import org.rucca.cheese.notification.resolver.toDTO
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationQueryService(
    private val notificationRepository: NotificationRepository,
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
    private val entityResolverFacade: NotificationEntityResolverFacade,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Retrieves notifications for the current user using cursor pagination. Resolves entity
     * references in metadata before mapping to DTOs.
     */
    suspend fun getNotificationsForCurrentUser(
        userId: IdType,
        cursorEncoded: String?,
        limit: Int,
        type: NotificationType?,
        read: Boolean?,
    ): Pair<List<NotificationDTO>, EncodedCursorPageDTO> {
        val effectiveLimit = limit.coerceIn(1, 100)

        val cursor: TypedCompositeCursor<Notification>? =
            cursorEncoded?.let {
                try {
                    TypedCompositeCursor.decode(it)
                } catch (e: Exception) {
                    log.warn("Failed to decode cursor '{}': {}", it, e.message)
                    null
                }
            }

        // Specification to filter notifications for the user, type, read status, and finalized
        // state
        val spec =
            Specification<Notification> { root, _, cb ->
                val predicates = mutableListOf(cb.equal(root.get<Long>("receiverId"), userId))
                type?.let { predicates.add(cb.equal(root.get<NotificationType>("type"), it)) }
                read?.let { predicates.add(cb.equal(root.get<Boolean>("read"), it)) }
                predicates.add(
                    cb.equal(root.get<Boolean>("finalized"), true)
                ) // Only show finalized notifications
                cb.and(*predicates.toTypedArray())
            }

        // Cursor specification for sorting (newest first)
        val cursorSpec =
            CursorSpecificationBuilder.compositeWithPath<Notification>("createdAt", "id")
                .sortByPath("createdAt", Sort.Direction.DESC)
                .sortByPath("id", Sort.Direction.DESC) // Tie-breaker
                .specification(spec)
                .build()

        // Step 1: Fetch Notification entities (using withContext if repository is blocking)
        val notificationPage =
            withContext(Dispatchers.IO) {
                notificationRepository.findAllWithCursor(cursorSpec, cursor, effectiveLimit)
            }

        if (notificationPage.content.isEmpty()) {
            return Pair(emptyList(), notificationPage.pageInfo.toPageDTO())
        }

        // Step 2: Parse metadata from all fetched notifications
        // Store alongside ID for easier mapping later
        val parsedMetadataList =
            notificationPage.content.mapNotNull { notification ->
                notification.metadata?.let { metaJson ->
                    try {
                        // Pair the ID with the parsed map
                        notification.id!! to objectMapper.readValue<Map<String, Any>>(metaJson)
                    } catch (e: Exception) {
                        log.error(
                            "Failed parsing metadata for notification {}: {}",
                            notification.id,
                            e.message,
                        )
                        null
                    }
                }
            }
        val metadataMapById = parsedMetadataList.toMap() // Map<Long, Map<String, Any>>

        // Step 3: Resolve all referenced entities in one batch
        val resolvedEntitiesBatch =
            if (metadataMapById.isNotEmpty()) {
                // Pass only the metadata maps to the resolver
                entityResolverFacade.resolveEntitiesFromMetadata(metadataMapById.values)
            } else {
                emptyMap() // No entities to resolve
            }

        // Step 4: Build DTO list using the extension function
        val dtoList =
            notificationPage.content.map { notification ->
                // Get the specific parsed metadata for this notification
                val metadataMap = metadataMapById[notification.id!!] ?: emptyMap()
                // Call the extension function, passing the batch of resolved entities
                notification.toDto(metadataMap, resolvedEntitiesBatch)
            }

        return Pair(dtoList, notificationPage.pageInfo.toPageDTO())
    }

    /**
     * Retrieves a single notification by ID, ensuring it belongs to the current user. Resolves
     * entity references before mapping to DTO.
     */
    @Transactional(readOnly = true)
    suspend fun getNotificationByIdForCurrentUser(
        userId: IdType,
        notificationId: Long,
    ): NotificationDTO {
        log.debug("Fetching notification {} for user {}", notificationId, userId)

        // Fetch the notification entity (use withContext if blocking)
        val notification =
            withContext(Dispatchers.IO) { notificationRepository.findById(notificationId) }
                .orElseThrow { NotFoundError("notification", notificationId) }

        // Verify ownership
        if (notification.receiverId != userId) {
            log.warn(
                "User {} attempted to access notification {} owned by user {}",
                userId,
                notificationId,
                notification.receiverId,
            )
            throw NotFoundError("notification", notificationId) // Not found for this user
        }

        // Resolve entities just for this one notification's metadata
        val metadataMap =
            notification.metadata?.let {
                try {
                    objectMapper.readValue<Map<String, Any>>(it)
                } catch (e: Exception) {
                    log.error(
                        "Failed parsing metadata for notification {}: {}",
                        notification.id,
                        e.message,
                    )
                    null
                }
            } ?: emptyMap()

        val resolvedEntitiesBatch =
            if (metadataMap.isNotEmpty()) {
                entityResolverFacade.resolveEntitiesFromMetadata(listOf(metadataMap))
            } else {
                emptyMap()
            }

        // Map to DTO using the extension function
        return notification.toDto(metadataMap, resolvedEntitiesBatch)
    }

    /** Gets the count of unread (and finalized) notifications for the current user. */
    fun getUnreadNotificationCountForCurrentUser(userId: IdType): Long {
        return notificationRepository.countByReceiverIdAndReadIsFalse(userId)
    }

    /** Retrieves the owner ID of a notification. Used for authorization checks. */
    fun getNotificationOwner(notificationId: Long): Long {
        // Fetching only the ID, no need for suspend/withContext unless findById is suspending
        return notificationRepository
            .findById(notificationId)
            .map { it.receiverId }
            .orElseThrow { NotFoundError("notification", notificationId) }
    }

    /** Checks if the given user owns the notification. Used for authorization checks. */
    fun isNotificationOwner(notificationId: Long?, userId: Long): Boolean {
        if (notificationId == null) return false
        return notificationRepository
            .findById(notificationId)
            .map { it.receiverId == userId }
            .orElse(false)
    }

    // --- Write Operations (State Changes) ---
    // These methods generally don't need the resolver facade or suspend modifier,
    // unless the return type requires the resolved DTO.

    /** Marks multiple notifications as read for the current user. */
    @Transactional
    fun markAsRead(userId: IdType, notificationIds: List<Long>): List<Long> {
        val updatedIds = mutableListOf<Long>()
        // Fetch only necessary fields if possible, or full entities
        val notificationsToUpdate =
            notificationRepository.findAllById(notificationIds).filter {
                it.receiverId == userId && !it.read
            } // Filter owned and unread

        if (notificationsToUpdate.isNotEmpty()) {
            notificationsToUpdate.forEach { it.read = true }
            notificationRepository.saveAll(notificationsToUpdate)
            updatedIds.addAll(notificationsToUpdate.mapNotNull { it.id })
            log.info("Marked {} notifications as read for user {}", updatedIds.size, userId)
        }

        // Log warnings for IDs not found or not owned (optional detailed check)
        val foundOwnedIds = notificationsToUpdate.mapNotNull { it.id }.toSet()
        notificationIds.forEach { reqId ->
            if (!foundOwnedIds.contains(reqId)) {
                // Check if it exists at all to differentiate
                if (!notificationRepository.existsById(reqId)) {
                    log.warn(
                        "Notification ID {} requested in markAsRead for user {} not found.",
                        reqId,
                        userId,
                    )
                } else {
                    log.warn(
                        "Notification ID {} requested in markAsRead for user {} is not owned by the user or already read.",
                        reqId,
                        userId,
                    )
                }
            }
        }

        return updatedIds
    }

    /** Marks all unread notifications as read for the current user. */
    @Transactional
    fun markAllAsReadForCurrentUser(userId: IdType): Int {
        val updatedCount = notificationRepository.markAllAsReadForReceiver(userId)
        log.info("Marked {} notifications as read for user {}", updatedCount, userId)
        return updatedCount
    }

    /** Deletes a notification for the current user (respecting soft-delete). */
    @Transactional
    fun deleteNotificationForCurrentUser(userId: Long, notificationId: Long) {
        // Fetch first to verify ownership before deleting
        val notification =
            notificationRepository.findByIdAndReceiverId(notificationId, userId).orElseThrow {
                NotFoundError("notification", notificationId)
            } // Throws if not found or not owned

        notificationRepository.delete(notification) // Assumes delete triggers @SQLDelete
        log.info("Deleted notification ID {} for user {}", notificationId, userId)
    }

    /** Updates the read status of a single notification, returning the updated DTO. */
    @Transactional
    suspend fun setReadStatus(
        userId: IdType,
        notificationId: Long,
        read: Boolean,
    ): NotificationDTO {
        log.info(
            "Setting read status to {} for notification {} for user {}",
            read,
            notificationId,
            userId,
        )

        val notification =
            withContext(Dispatchers.IO) { notificationRepository.findById(notificationId) }
                .orElseThrow { NotFoundError("notification", notificationId) }

        // Verify ownership
        if (notification.receiverId != userId) {
            log.warn(
                "User {} attempted to update read status for notification {} owned by user {}",
                userId,
                notificationId,
                notification.receiverId,
            )
            throw NotFoundError("notification", notificationId)
        }

        val updatedNotification: Notification
        if (notification.read != read) {
            notification.read = read
            updatedNotification =
                withContext(Dispatchers.IO) { notificationRepository.save(notification) }
            log.debug("Notification {} read status updated to {}", notificationId, read)
        } else {
            log.debug(
                "Notification {} read status already {}, no update needed",
                notificationId,
                read,
            )
            updatedNotification = notification // Use current state if no update occurred
        }

        // Resolve entities for the DTO response
        val metadataMap =
            updatedNotification.metadata?.let {
                try {
                    objectMapper.readValue<Map<String, Any>>(it)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyMap()
        val resolvedEntities =
            if (metadataMap.isNotEmpty())
                entityResolverFacade.resolveEntitiesFromMetadata(listOf(metadataMap))
            else emptyMap()
        return updatedNotification.toDto(metadataMap, resolvedEntities)
    }

    /** Updates the read status for multiple notifications, returning the list of updated IDs. */
    @Transactional
    fun bulkSetReadStatus(userId: IdType, updates: List<Pair<Long, Boolean>>): List<Long> {
        if (updates.isEmpty()) return emptyList()

        val requestedIds = updates.map { it.first }
        val desiredStatusMap = updates.toMap()

        log.info(
            "Bulk setting read status for {} notifications for user {}",
            requestedIds.size,
            userId,
        )

        // Fetch only notifications owned by the user among the requested IDs
        val ownedNotifications =
            notificationRepository.findAllById(requestedIds).filter { it.receiverId == userId }

        val notificationsToSave = mutableListOf<Notification>()
        val updatedIds = mutableListOf<Long>()

        ownedNotifications.forEach { notification ->
            val desiredStatus = desiredStatusMap[notification.id!!]
            if (desiredStatus != null && notification.read != desiredStatus) {
                notification.read = desiredStatus
                notificationsToSave.add(notification)
                updatedIds.add(notification.id!!)
            }
        }

        // Log warnings for IDs not found or not owned (optional)
        val ownedIdsSet = ownedNotifications.mapNotNull { it.id }.toSet()
        requestedIds.forEach { reqId ->
            if (!ownedIdsSet.contains(reqId)) {
                log.warn(
                    "Requested notification ID {} in bulk update for user {} was not found or not owned.",
                    reqId,
                    userId,
                )
            }
        }

        if (notificationsToSave.isNotEmpty()) {
            notificationRepository.saveAll(notificationsToSave)
            log.info(
                "Successfully updated read status for {} notifications for user {}",
                updatedIds.size,
                userId,
            )
        } else {
            log.info("No notifications required status updates in bulk request for user {}", userId)
        }

        return updatedIds
    }
}

fun Notification.toDto(
    metadataMap: Map<String, Any>, // Parsed metadata for this notification
    resolvedEntitiesBatch:
        Map<String, Map<String, ResolvableEntityInfo?>>, // ALL resolved entities for the batch
): NotificationDTO {
    val entities = mutableMapOf<String, ResolvableEntityInfo>()

    // Iterate through metadata, looking for entity reference structures
    metadataMap.forEach { (key, value) ->
        if (value is Map<*, *>) { // Found a potential entity reference
            val entityType = value["type"] as? String
            val entityId = value["id"] as? String // ID is String now

            if (entityType != null && entityId != null) {
                // Look up the pre-resolved entity info from the batch map
                val resolvedInfo = resolvedEntitiesBatch[entityType]?.get(entityId)
                // Use the metadata key ("actor", "team", etc.) as the DTO key
                if (resolvedInfo != null) entities[key] = resolvedInfo
            }
        }
    }

    // Extract non-entity context metadata
    val contextMeta =
        metadataMap.filterValues { it !is Map<*, *> || it["type"] == null || it["id"] == null }

    return NotificationDTO(
        id = this.id!!,
        type = this.type.toDTO(), // Use existing type mapping if needed
        read = this.read,
        createdAt = this.createdAt.toEpochMilli() ?: 0L,
        entities = entities.mapValues { (_, value) -> value.toDTO() },
        contextMetadata = contextMeta,
    )
}
