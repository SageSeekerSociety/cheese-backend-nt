package org.rucca.cheese.notification

import javax.annotation.PostConstruct
import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class NotificationController(
    private val notificationService: NotificationService,
    private val authorizationService: AuthorizationService,
    private val authenticationService: AuthenticationService,
    @Autowired private val environment: Environment,
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
        if (
            !environment.activeProfiles.contains("dev") &&
                !environment.activeProfiles.contains("test")
        ) {
            throw BadRequestError("This operation is not allowed in production environment")
        }
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
        type: kotlin.String?,
        read: kotlin.Boolean?,
    ): ResponseEntity<ListNotifications200ResponseDTO> {
        val notifications =
            notificationService.listNotifications(
                when (type) {
                    null -> null
                    else -> NotificationType.fromString(type)
                },
                read,
                pageStart,
                pageSize,
            )
        return ResponseEntity.ok(
            ListNotifications200ResponseDTO(
                200,
                "success",
                ListNotifications200ResponseDataDTO(notifications.first, notifications.second),
            )
        )
    }

    @Guard("mark-as-read", "notification")
    override fun markNotificationsAsRead(
        markNotificationsAsReadRequestDTO: MarkNotificationsAsReadRequestDTO
    ): ResponseEntity<MarkNotificationsAsRead200ResponseDTO> {
        notificationService.markAsRead(markNotificationsAsReadRequestDTO.notificationIds)
        return ResponseEntity.ok(
            MarkNotificationsAsRead200ResponseDTO(
                200,
                "success",
                MarkNotificationsAsRead200ResponseDataDTO(
                    markNotificationsAsReadRequestDTO.notificationIds
                ),
            )
        )
    }

    @Guard("get-unread-count", "notification")
    override fun getUnreadNotificationsCount(
        receiverId: kotlin.Long
    ): ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        return ResponseEntity.ok(
            GetUnreadNotificationsCount200ResponseDTO(
                200,
                "success",
                GetUnreadNotificationsCount200ResponseDataDTO(
                    notificationService.getUnreadCount(receiverId)
                ),
            )
        )
    }
}
