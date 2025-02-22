package org.rucca.cheese.notification

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import java.util.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

enum class NotificationType(@JsonValue val type: String) {
    MENTION("mention"),
    REPLY("reply"),
    REACTION("reaction"),
    PROJECT_INVITE("project_invite"),
    DEADLINE_REMIND("deadline_remind");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): NotificationType {
            return entries.find { it.type.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown notification type: $value")
        }
    }
}

data class NotificationContent(
    val text: String,
    val projectId: Long?,
    val discussionId: Long?,
    val knowledgeId: Long?,
)

@Entity
@Table(name = "notification")
@SQLRestriction("deleted_at IS NULL")
class Notification(
    //    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: IdType? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var type: NotificationType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    var receiver: User,
    //    @Column(columnDefinition = "jsonb", nullable = false)
    //    var content: String, // JSON 格式存储 NotificationContent
    @Column(nullable = false) var content: String,
    @Column(nullable = false) var read: Boolean = false,
    //    @Column(name = "created_at", nullable = false) var createdAt: Long =
    // System.currentTimeMillis(),
) : BaseEntity()

interface NotificationRepository : JpaRepository<Notification, IdType> {

    //    fun findByIdAndReceiverId(id: IdType, receiverId: Long): Optional<Notification>

    //    fun findAllByReceiverId(receiverId: Long): List<Notification>

    //    fun countByReceiverIdAndRead(receiverId: Long, read: Boolean): Int

    //    fun findFirstByReceiverIdAndTypeAndRead(
    //        receiverId: Long,
    //        type: NotificationType?,
    //        read: Boolean?,
    //    ): Notification?

    fun findByIdAndReceiver(id: IdType, receiver: User): Optional<Notification>

    fun findAllByReceiver(receiver: User): List<Notification>

    fun countByReceiverAndRead(receiver: User, read: Boolean): Int

    fun findFirstByReceiver(receiver: User): Notification?

    fun findFirstByReceiverAndType(receiver: User, type: NotificationType): Notification?

    fun findFirstByReceiverAndRead(receiver: User, read: Boolean): Notification?

    fun findFirstByReceiverAndTypeAndRead(
        receiver: User,
        type: NotificationType?,
        read: Boolean?,
    ): Notification?

    fun findAllByReceiverAndTypeAndRead(
        receiver: User,
        type: NotificationType? = null,
        read: Boolean? = null,
    ): List<Notification>
}
