package org.rucca.cheese.notification

import java.util.*
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.NotificationDTO
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.PostNotificationRequestContentDTO
import org.springframework.stereotype.Service

@Service
open class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val authenticateService: AuthenticationService,
) {

    fun getNotificationDTO(notificationId: IdType): NotificationDTO {
        val notification = notificationRepository.findById(notificationId)
        if (!notification.isPresent) {
            throw NotFoundError("notification", notificationId)
        }
        return notification.get().toNotificationDTO(notification.get())
    }

    fun listNotifications(
        type: NotificationType? = null,
        read: Boolean? = null,
        pageStart: IdType?,
        pageSize: Int,
    ): Pair<List<NotificationDTO>, PageDTO> {
        val actualPageStart =
            when {
                pageStart == null || pageStart == 0L -> {
                    notificationRepository
                        .findFirstByReceiverIdAndTypeAndReadOrderByIdAsc(
                            authenticateService.getCurrentUserId(),
                            type,
                            read,
                        )
                        ?.id ?: return Pair(emptyList(), PageDTO(0, 0, false, false)) // 如果找不到，返回空
                }
                else -> pageStart
            }

        val notification = notificationRepository.findById(actualPageStart)
        if (!notification.isPresent) {
            throw NotFoundError("notification", actualPageStart)
        }
        val notifications =
            notificationRepository.findAllByReceiverIdAndTypeAndRead(
                notification.get().receiverId,
                type,
                read,
            )
        val (curr, page) =
            PageHelper.pageFromAll(
                notifications,
                actualPageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("notification", id) },
            )
        return Pair(curr.map { it.toNotificationDTO(it) }, page)
    }

    fun createNotification(
        type: NotificationType,
        receiverId: Long,
        text: String,
        projectId: Long? = null,
        discussionId: Long? = null,
        knowledgeId: Long? = null,
    ): IdType {
        val notification =
            notificationRepository.save(
                Notification(
                    type = type,
                    receiverId = receiverId,
                    content =
                        NotificationContent(
                            text = text,
                            projectId = projectId,
                            discussionId = discussionId,
                            knowledgeId = knowledgeId,
                        ),
                    read = false,
                )
            )
        return notification.id!!
    }

    fun deleteNotification(notificationId: Long) {
        val notification =
            notificationRepository.findById(notificationId).orElseThrow {
                NotFoundError("notification", notificationId)
            }
        notificationRepository.delete(notification)
    }

    fun markAsRead(notificationIds: List<Long>) {
        val notification = mutableListOf<Notification>()
        for (id in notificationIds) {
            val entity: Optional<Notification> =
                notificationRepository.findByIdAndReceiverId(
                    id,
                    authenticateService.getCurrentUserId(),
                )
            if (entity.isPresent) {
                notification.add(entity.get())
            } else {
                //                throw NotFoundError("notification", id)
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

    fun isNotificationAdmin(notificationId: IdType?, userId: IdType): Boolean {
        require(notificationId != null) { "notificationId cannot be null for this operation" }
        val notification =
            notificationRepository.findByIdAndReceiverId(notificationId, userId).orElseThrow {
                NotFoundError("notification", notificationId)
            }

        return notification.receiverId == authenticateService.getCurrentUserId()
    }

    fun Notification.toNotificationDTO(notification: Notification): NotificationDTO {
        return NotificationDTO(
            id = notification.id!!,
            type = NotificationDTO.Type.valueOf(notification.type.name),
            read = notification.read,
            receiverId = notification.receiverId,
            content = notification.content.toNotificationContentDTO(notification.content),
            createdAt = notification.createdAt!!.toEpochMilli(),
        )
    }

    fun NotificationContent.toNotificationContentDTO(
        content: NotificationContent
    ): PostNotificationRequestContentDTO {
        return PostNotificationRequestContentDTO(
            content.text,
            content.projectId,
            content.discussionId,
            content.knowledgeId,
        )
    }
}
