package org.rucca.cheese.notification

import javax.annotation.PostConstruct
import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.auth.spring.UseOldAuth
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseOldAuth
class NotificationController(
    private val notificationService: NotificationService,
    private val authorizationService: AuthorizationService,
    private val jwtService: JwtService,
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

    @Guard("delete", "notification")
    override fun deleteNotification(
        @ResourceId notificationId: kotlin.Long
    ): ResponseEntity<CommonResponseDTO> {
        notificationService.deleteNotification(notificationId)
        return ResponseEntity.ok(CommonResponseDTO(200, "ok"))
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
                type = if (type == null) null else NotificationType.fromString(type),
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
