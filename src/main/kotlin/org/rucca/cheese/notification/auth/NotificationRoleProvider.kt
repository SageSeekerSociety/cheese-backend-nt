package org.rucca.cheese.notification.auth

import org.rucca.cheese.auth.context.DomainContextKeys
import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.notification.services.NotificationQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationRoleProvider(private val notificationQueryService: NotificationQueryService) :
    DomainRoleProvider {

    private val logger = LoggerFactory.getLogger(NotificationRoleProvider::class.java)

    override val domain: Domain = NotificationDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val resourceType =
            DomainContextKeys.RESOURCE_TYPE.get(context)?.let { NotificationResource.of(it) }
        val resourceId = DomainContextKeys.RESOURCE_ID.get(context)

        // If no resource type matches, return empty roles
        if (resourceType != NotificationResource.NOTIFICATION) {
            logger.trace("Not a notification resource type in context for user {}", userId)
            return emptySet()
        }

        // Case 1: Operating on a specific notification (VIEW, UPDATE, DELETE single)
        if (resourceId != null) {
            // Check if the current user owns this specific notification ID
            // Use the existing service method used by the controller auth registration logic
            return try {
                // isNotificationOwner checks existence and ownership
                if (notificationQueryService.isNotificationOwner(resourceId, userId)) {
                    logger.debug(
                        "User {} IS owner of notification {}, granting OWNER role",
                        userId,
                        resourceId,
                    )
                    setOf(NotificationRole.OWNER)
                } else {
                    logger.debug("User {} is NOT owner of notification {}", userId, resourceId)
                    emptySet() // Not the owner
                }
            } catch (e: Exception) { // Catch potential NotFoundError or other issues from
                // isNotificationOwner
                logger.debug(
                    "Error checking ownership for notification {} for user {}: {}",
                    resourceId,
                    userId,
                    e.message,
                )
                emptySet() // Treat errors as non-owner for safety
            }
        }
        // Case 2: Operating on the collection (LIST, bulk UPDATE, UPDATE_STATUS, VIEW_COUNT)
        else {
            // For operations on the collection, the user is implicitly acting on their *own* set.
            // Grant OWNER role. The actual filtering/operation scope is handled by the service
            // logic (using AuthUtils).
            logger.debug(
                "Granting implicit OWNER role for collection operation for user {}",
                userId,
            )
            return setOf(NotificationRole.OWNER)
        }
    }
}
