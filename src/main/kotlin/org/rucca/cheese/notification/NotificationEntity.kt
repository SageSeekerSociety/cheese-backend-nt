package org.rucca.cheese.notification

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import java.util.*
import org.hibernate.annotations.SQLRestriction
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

// enum class NotificationType(@JsonValue val value: kotlin.String) {
//    mention("mention"),
//    reply("reply"),
//    reaction("reaction"),
//    project_invite("project_invite"),
//    deadline_remind("deadline_remind");
//
//    companion object {
//        @JvmStatic
//        @JsonCreator(mode = JsonCreator.Mode.DELEGATING) // 关键！
//        fun forValue(value: kotlin.String): NotificationType {
//            return values().find { it.value == value }
//                ?: throw IllegalArgumentException("Unknown notification type: $value")
//        }
//    }
// }
//
// @Entity
// @SQLRestriction("deleted_at IS NULL")
// class Notification(
//    @Column(nullable = false) val type: NotificationType,
//    @Column(nullable = false) val receiverId: Long,
//    @Column(nullable = true) var content: NotificationContent,
//    @Column(nullable = false) var read: Boolean = false,
// ) : BaseEntity()
//
// @Embeddable
// class NotificationContent() {
//    var text: String = ""
//    var projectId: Long? = null
//    var discussionId: Long? = null
//    var knowledgeId: Long? = null
//
//    constructor(text: String, projectId: Long?, discussionId: Long?, knowledgeId: Long?) : this()
// {
//        this.text = text
//        this.projectId = projectId
//        this.discussionId = discussionId
//        this.knowledgeId = knowledgeId
//    }
// }
//
// interface NotificationRepository : JpaRepository<Notification, IdType> {
//
//    fun findByIdAndReceiverId(id: IdType, receiverId: Long): Optional<Notification>
//
//    fun findAllByReceiverIdAndTypeAndRead(
//        receiverId: Long,
//        type: NotificationType? = null,
//        read: Boolean? = null,
//    ): List<Notification>
//
//    fun findAllByReceiverId(receiverId: Long): List<Notification>
//
//    fun findAllByReceiverIdAndRead(receiverId: Long, read: Boolean): List<Notification>
//
//    fun countByReceiverIdAndRead(receiverId: Long, read: Boolean): Long
//
//    fun findFirstByReceiverIdAndTypeAndReadOrderByIdAsc(
//        receiverId: Long,
//        type: NotificationType?,
//        read: Boolean?,
//    ): Notification?
// }

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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: IdType? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var type: NotificationType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    var receiver: User,
    @Column(columnDefinition = "jsonb", nullable = false)
    var content: String, // JSON 格式存储 NotificationContent
    @Column(nullable = false) var read: Boolean = false,
    @Column(name = "created_at", nullable = false) var createdAt: Long = System.currentTimeMillis(),
)

interface NotificationRepository : JpaRepository<Notification, IdType> {

    fun findByIdAndReceiverId(id: IdType, receiverId: Long): Optional<Notification>

    fun countByReceiverIdAndRead(receiverId: Long, read: Boolean): Long

    fun findFirstByReceiverIdAndTypeAndReadOrderByIdAsc(
        receiverId: Long,
        type: NotificationType?,
        read: Boolean?,
    ): Notification?

    fun findAllByReceiverIdAndTypeAndRead(
        receiverId: Long,
        type: NotificationType?,
        read: Boolean?,
    ): List<Notification>

    fun findByIdAndReceiver(id: IdType, receiver: User): Optional<Notification>

    fun findAllByReceiverAndTypeAndRead(
        receiver: User,
        type: NotificationType? = null,
        read: Boolean? = null,
    ): List<Notification>

    fun countByReceiverAndRead(receiver: User, read: Boolean): Long
}
