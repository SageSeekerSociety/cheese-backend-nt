package org.rucca.cheese.notification

import javax.annotation.PostConstruct
import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class NotificationController(
    private val notificationService: NotificationService,
    private val authorizationService: AuthorizationService,
    private val authenticationService: AuthenticationService,
) : NotificationsApi {

    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register(
            "notification",
            notificationService::getNotificationOwner,
        )

        authorizationService.customAuthLogics.register("is-notification-owner") {
            userId: IdType,
            action: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any?>?,
            _: IdGetter?,
            _: Any? ->
            when (action) {
                "delete" -> {
                    if (resourceId == null) {
                        return@register false
                    }
                    return@register notificationService.isNotificationAdmin(resourceId, userId)
                }
                else -> {
                    return@register true
                }
            }
        }
    }

    @Guard("create", "notification")
    override fun postNotification(
        postNotificationRequestDTO: PostNotificationRequestDTO
    ): ResponseEntity<PostNotification200ResponseDTO> {
        val notificationId =
            notificationService.createNotification(
                NotificationType.fromString(postNotificationRequestDTO.type.value),
                postNotificationRequestDTO.receiverId,
                postNotificationRequestDTO.content.text?.ifBlank { " " } ?: " ",
                postNotificationRequestDTO.content.projectId,
                postNotificationRequestDTO.content.discussionId,
                postNotificationRequestDTO.content.knowledgeId,
            )
        val notificationDTO = notificationService.getNotificationDTO(notificationId)
        return ResponseEntity.ok(
            PostNotification200ResponseDTO(
                200,
                PostNotification200ResponseDataDTO(notificationDTO),
                "ok",
            )
        )
    }

    @Guard("delete", "notification")
    override fun deleteNotification(
        @ResourceId notificationId: kotlin.Long
    ): ResponseEntity<DeleteNotification200ResponseDTO> {
        notificationService.deleteNotification(notificationId)
        return ResponseEntity.ok(DeleteNotification200ResponseDTO(200, "ok"))
    }

    @Guard("list-notifications", "notification")
    override fun listNotifications(
        pageStart: kotlin.Long,
        pageSize: kotlin.Int,
        type: kotlin.Any?,
        read: kotlin.Boolean?,
    ): ResponseEntity<ListNotifications200ResponseDTO> {
        val notifications =
            notificationService.listNotifications(
                when (type) {
                    null -> null
                    else -> NotificationType.valueOf(type.toString())
                },
                read,
                pageStart,
                pageSize,
            )
        return ResponseEntity.ok(
            ListNotifications200ResponseDTO(
                0,
                "success",
                ListNotifications200ResponseDataDTO(notifications.first, notifications.second),
            )
        )
    }

    @Guard("mark-as-read", "notification")
    override fun markNotificationsAsRead(
        markNotificationsAsReadRequestDTO: MarkNotificationsAsReadRequestDTO
    ): ResponseEntity<kotlin.Any> {
        notificationService.markAsRead(markNotificationsAsReadRequestDTO.notificationIds)
        return ResponseEntity.ok("Notifications marked as read")
    }

    @Guard("get-unread-count", "notification")
    override fun getUnreadNotificationsCount(
        getUnreadNotificationsCountRequestDTO: GetUnreadNotificationsCountRequestDTO
    ): ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        return ResponseEntity.ok(
            GetUnreadNotificationsCount200ResponseDTO(
                0,
                "success",
                GetUnreadNotificationsCount200ResponseDataDTO(
                    notificationService.getUnreadCount(
                        getUnreadNotificationsCountRequestDTO.receiverId
                    )
                ),
            )
        )
    }
}
