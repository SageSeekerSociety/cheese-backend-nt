package org.rucca.cheese.notification

import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity

class NotificationController(private val notificationService: NotificationService) :
    NotificationsApi {

    fun createNotification(notification: Notification) {
        notificationService.createNotification(notification)
    }

    fun deleteNotification(notificationId: Long) {
        notificationService.deleteNotification(notificationId)
    }

    fun listNotifications(
        notificationsGetRequestDTO: NotificationsGetRequestDTO
    ): ResponseEntity<NotificationsGet200ResponseDTO> {
        val notifications =
            notificationService.listNotifications(
                NotificationType.valueOf(notificationsGetRequestDTO.type.name),
                notificationsGetRequestDTO.read,
                notificationsGetRequestDTO.pageStart,
                notificationsGetRequestDTO.pageSize
            )
        return ResponseEntity.ok(
            NotificationsGet200ResponseDTO(
                0,
                "success",
                NotificationsGet200ResponseDataDTO(notifications.first, notifications.second)
            )
        )
    }

    fun markAsRead(
        notificationsReadPostRequestDTO: NotificationsReadPostRequestDTO,
    ) {
        notificationService.markAsRead(notificationsReadPostRequestDTO.notificationIds)
    }

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
