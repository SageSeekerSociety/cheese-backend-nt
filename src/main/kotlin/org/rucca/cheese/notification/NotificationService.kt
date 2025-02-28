package org.rucca.cheese.notification

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.NotificationContentDTO
import org.rucca.cheese.model.NotificationDTO
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
open class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userService: UserService,
    private val authenticateService: AuthenticationService,
    private val objectMapper: ObjectMapper,
) {

    fun getNotificationDTO(notificationId: IdType): NotificationDTO {
        val notification = notificationRepository.findById(notificationId)
        if (!notification.isPresent) {
            throw NotFoundError("notification", notificationId)
        }
        return notification.get().toNotificationDTO()
    }

    fun listNotifications(
        type: NotificationType? = null,
        read: Boolean? = null,
        pageStart: IdType?,
        pageSize: Int,
    ): Pair<List<NotificationDTO>, PageDTO> {
        val currentUser = User().apply { id = authenticateService.getCurrentUserId().toInt() }

        val actualPageStart =
            when {
                pageStart == null || pageStart == 0L -> {
                    val firstNotification =
                        when {
                            type == null && read == null ->
                                notificationRepository.findFirstByReceiver(currentUser)
                            type != null && read == null ->
                                notificationRepository.findFirstByReceiverAndType(currentUser, type)
                            type == null && read != null ->
                                notificationRepository.findFirstByReceiverAndRead(currentUser, read)
                            else ->
                                notificationRepository.findFirstByReceiverAndTypeAndRead(
                                    currentUser,
                                    type,
                                    read,
                                )
                        }
                    firstNotification?.id
                        ?: return Pair(emptyList(), PageDTO(0, pageSize, false, false))
                }
                else -> pageStart
            }

        notificationRepository.findById(actualPageStart).orElseThrow {
            NotFoundError("notification", actualPageStart)
        }

        val notifications =
            notificationRepository
                .findAllByReceiver(currentUser)
                .filter { type == null || it.type == type }
                .filter { read == null || it.read == read }

        val (curr, page) =
            PageHelper.pageFromAll(
                notifications,
                actualPageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("notification", id) },
            )

        return Pair(curr.map { it.toNotificationDTO() }, page)
    }

    fun createNotification(
        type: NotificationType,
        receiverId: Long,
        text: String,
        projectId: Long? = null,
        discussionId: Long? = null,
        knowledgeId: Long? = null,
    ): IdType {
        if (!userService.existsUser(receiverId)) {
            throw NotFoundError("user", receiverId)
        }
        val notification =
            notificationRepository.save(
                Notification(
                    type = type,
                    receiver = User().apply { id = receiverId.toInt() },
                    content = NotificationContent(text, projectId, discussionId, knowledgeId),
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
        for (notificationId in notificationIds) {
            val entity =
                notificationRepository.findByIdAndReceiver(
                    notificationId,
                    User().apply { id = authenticateService.getCurrentUserId().toInt() },
                )
            if (entity.isPresent) {
                notification.add(entity.get())
            } else {
                throw NotFoundError("Notification not found", notificationId)
            }
        }
        notification.forEach { it.read = true }
        notificationRepository.saveAll(notification)
    }

    fun getUnreadCount(receiverId: Long): Int {
        return notificationRepository.countByReceiverAndRead(
            User().apply { id = receiverId.toInt() },
            false,
        )
    }

    fun getNotificationOwner(notificationId: IdType): IdType {
        val notification = notificationRepository.findById(notificationId)
        if (!notification.isPresent) {
            throw NotFoundError("notification", notificationId)
        }
        return notification.get().receiver.id!!.toLong()
    }

    fun isNotificationAdmin(notificationId: IdType?, userId: IdType): Boolean {
        require(notificationId != null) { "notificationId cannot be null for this operation" }
        val notification =
            notificationRepository
                .findByIdAndReceiver(notificationId, User().apply { id = userId.toInt() })
                .orElseThrow { NotFoundError("notification", notificationId) }

        return notification.receiver.id!!.toLong() == authenticateService.getCurrentUserId()
    }

    fun Notification.toNotificationDTO(): NotificationDTO {
        return NotificationDTO(
            id = this.id!!,
            type = NotificationDTO.Type.valueOf(this.type.name),
            read = this.read,
            receiverId = this.receiver.id!!.toLong(),
            content =
                NotificationContentDTO(
                    this.content.text,
                    this.content.projectId,
                    this.content.discussionId,
                    this.content.knowledgeId,
                ),
            createdAt = this.createdAt!!.toEpochMilli(),
        )
    }
}
