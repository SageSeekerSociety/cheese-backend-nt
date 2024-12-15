package org.rucca.cheese.notification

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository

enum class NotificationType(type: String) {
    MENTION("mention"),
    REPLY("reply"),
    REACTION("reaction"),
    PROJECT_INVITE("project_invite"),
    DEADLINE_REMIND("deadline_remind")
}

@Entity
@SQLRestriction("deleted_at IS NULL")
class Notification(
    val type: NotificationType,
    val receiverId: Long,
    var content: NotificationContent,
    var read: Boolean = false,
) : BaseEntity()

@Embeddable
class NotificationContent(
    val text: String,
    val projectId: Long,
    val discussionId: Long,
    val knowledgeId: Long
)

interface NotificationRepository : JpaRepository<Notification, IdType> {

    fun findAllByIdAndReceiverIdAndTypeAndRead(
        id: Long,
        receiverId: Long,
        type: NotificationType? = null,
        read: Boolean? = null
    ): List<Notification>

    fun findAllByReceiverId(receiverId: Long): List<Notification>

    fun findAllByReceiverIdAndRead(receiverId: Long, read: Boolean): List<Notification>

    fun countByReceiverIdAndRead(receiverId: Long, read: Boolean): Long
}
