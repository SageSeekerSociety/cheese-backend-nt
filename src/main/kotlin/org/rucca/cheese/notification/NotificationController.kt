package org.rucca.cheese.notification

import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class NotificationController(private val notificationService: NotificationService) :
    NotificationsApi {

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
    fun markAsRead(
        notificationsReadPostRequestDTO: NotificationsReadPostRequestDTO,
    ) {
        notificationService.markAsRead(notificationsReadPostRequestDTO.notificationIds)
    }

    @PostMapping("/notifications/unread/count")
    fun getUnreadCount(
        notificationsUnreadCountGetRequestDTO: NotificationsUnreadCountGetRequestDTO,
    ): ResponseEntity<NotificationsUnreadCountGet200ResponseDTO> {
        return ResponseEntity.ok(
            NotificationsUnreadCountGet200ResponseDTO(
                0,
                "success",
                NotificationsUnreadCountGet200ResponseDataDTO(
                    notificationService.getUnreadCount(
                        notificationsUnreadCountGetRequestDTO.receiverId
                    )
                )
            )
        )
    }
}
