package org.rucca.cheese.notification

import java.util.*
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.NotificationContentDTO
import org.rucca.cheese.model.NotificationDTO
import org.rucca.cheese.model.PageDTO
import org.springframework.stereotype.Service

@Service
open class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val authenticateService: AuthenticationService,
) {

    fun listNotifications(
        type: NotificationType? = null,
        read: Boolean? = null,
        pageStart: IdType,
        pageSize: Int,
    ): Pair<List<NotificationDTO>, PageDTO> {
        val notification = notificationRepository.findById(pageStart)
        if (!notification.isPresent) {
            throw NotFoundError("notification", pageStart)
        }
        val notifications =
            notificationRepository.findAllByReceiverIdAndTypeAndRead(
                notification.get().receiverId,
                type,
                read
            )
        val (curr, page) =
            PageHelper.pageFromAll(
                notifications,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("notification", id) }
            )
        return Pair(curr.map { it.toNotificationDTO(it) }, page)
    }

    fun createNotification(notification: Notification) {
        notificationRepository.save(notification)
    }

    fun deleteNotification(notificationId: Long) {
        notificationRepository.delete(notificationRepository.findById(notificationId).get())
    }

    fun markAsRead(notificationIds: List<Long>) {
        val notification = mutableListOf<Notification>()
        for (id in notificationIds) {
            val entity: Optional<Notification> = notificationRepository.findById(id)
            if (entity.isPresent) {
                notification.add(entity.get())
            } else {
                throw NotFoundError("notification", id)
            }
        }
        notification.forEach { it.read = true }
        notificationRepository.saveAll(notification)
    }

    fun getUnreadCount(receiverId: Long): Int {
        return notificationRepository.countByReceiverIdAndRead(receiverId, false).toInt()
    }

    fun getNotificationOwner(notificationId: IdType): IdType {
        val notification = notificationRepository.findById(notificationId)
        if (!notification.isPresent) {
            throw NotFoundError("notification", notificationId)
        }
        return notification.get().receiverId
    }

    fun isNotificationAdmin(notificationId: IdType, userId: IdType): Boolean {
        val notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
        if (!notification.isPresent) {
            throw NotFoundError("notification", notificationId)
        }
        return notification.get().receiverId == authenticateService.getCurrentUserId()
    }

    fun Notification.toNotificationDTO(notification: Notification): NotificationDTO {
        return NotificationDTO(
            id = notification.id!!,
            type = NotificationDTO.Type.valueOf(notification.type.name),
            read = notification.read,
            receiverId = notification.receiverId,
            content = notification.content.toNotificationContentDTO(notification.content),
            createdAt = notification.createdAt!!.toEpochMilli()
        )
    }

    fun NotificationContent.toNotificationContentDTO(
        content: NotificationContent
    ): NotificationContentDTO {
        return NotificationContentDTO(
            content.text,
            content.projectId,
            content.discussionId,
            content.knowledgeId
        )
    }
}
