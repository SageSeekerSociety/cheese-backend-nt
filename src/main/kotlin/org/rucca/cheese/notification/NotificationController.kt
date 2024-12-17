package org.rucca.cheese.notification

import javax.annotation.PostConstruct
import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.Guard
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
            "notifications",
            notificationService::getNotificationOwner
        )
        authorizationService.customAuthLogics.register("is-notification-admin") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any?>?,
            _: IdGetter?,
            _: Any?,
            ->
            notificationService.isNotificationAdmin(
                resourceId ?: throw IllegalArgumentException("resourceId is null"),
                userId
            )
        }
    }

    fun createNotification(notification: Notification) {
        notificationService.createNotification(notification)
    }

    fun deleteNotification(notificationId: Long) {
        notificationService.deleteNotification(notificationId)
    }

    @PostMapping("/notifications")
    fun listNotifications(
        @RequestParam("type") type: String?,
        @RequestParam("read") read: Boolean?,
        @RequestParam("page_start") pageStart: Long,
        @RequestParam("page_size") pageSize: Int,
    ): ResponseEntity<ListNotifications200ResponseDTO> {
        val notifications =
            notificationService.listNotifications(
                when (type) {
                    null -> null
                    else -> NotificationType.valueOf(type)
                },
                read,
                pageStart,
                pageSize
            )
        return ResponseEntity.ok(
            ListNotifications200ResponseDTO(
                0,
                "success",
                ListNotifications200ResponseDataDTO(notifications.first, notifications.second)
            )
        )
    }

    @PostMapping("/notifications/read")
    @Guard("mark-as-read", "notifications")
    fun markAsRead(markNotificationsAsReadRequestDTO: MarkNotificationsAsReadRequestDTO) {
        notificationService.markAsRead(markNotificationsAsReadRequestDTO.notificationIds)
    }

    @PostMapping("/notifications/unread/count")
    @Guard("get-unread-count", "notifications")
    fun getUnreadCount(
        getUnreadNotificationsCountRequestDTO: GetUnreadNotificationsCountRequestDTO,
    ): ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        return ResponseEntity.ok(
            GetUnreadNotificationsCount200ResponseDTO(
                0,
                "success",
                GetUnreadNotificationsCount200ResponseDataDTO(
                    notificationService.getUnreadCount(
                        getUnreadNotificationsCountRequestDTO.receiverId
                    )
                )
            )
        )
    }
}
