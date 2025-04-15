package org.rucca.cheese.notification.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.NotificationTypeDTO

enum class NotificationType(@JsonValue val type: String) {
    MENTION("mention"),
    REPLY("reply"),
    REACTION("reaction"),
    PROJECT_INVITE("project_invite"),
    DEADLINE_REMIND("deadline_remind"),

    // --- Team Membership Application Types ---
    /** When a user requests to join a team (Sent to Admins/Owners) */
    TEAM_JOIN_REQUEST("team_join_request"),
    /** When an admin/owner invites a user (Sent to Invited User) */
    TEAM_INVITATION("team_invitation"),
    /** When a join request is approved (Sent to Requester) */
    TEAM_REQUEST_APPROVED("team_request_approved"),
    /** When a join request is rejected (Sent to Requester) */
    TEAM_REQUEST_REJECTED("team_request_rejected"),
    /** When an invitation is accepted (Sent to Inviter) */
    TEAM_INVITATION_ACCEPTED("team_invitation_accepted"),
    /** When an invitation is declined (Sent to Inviter) */
    TEAM_INVITATION_DECLINED("team_invitation_declined"),
    /** When an invitation is cancelled by admin (Sent to Invited User) */
    TEAM_INVITATION_CANCELED("team_invitation_canceled"), // Optional but good
    /** When a join request is cancelled by user (Sent to Admins/Owners) */
    TEAM_REQUEST_CANCELED("team_request_canceled"); // Optional but good

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): NotificationType {
            return entries.find { it.type.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown notification type: $value")
        }
    }
}

enum class NotificationChannel {
    IN_APP,
    EMAIL,
}

@Entity
@Table(
    name = "notification",
    indexes =
        [
            Index(
                name = "idx_notification_receiver_read_created",
                columnList = "receiverId, read, createdAt DESC",
            ),
            Index(
                name = "idx_notification_aggregation",
                columnList = "receiverId, aggregationKey, aggregateUntil",
            ),
        ],
)
@SQLDelete(sql = "UPDATE notification SET deleted_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
class Notification(
    @Column(name = "receiver_id", nullable = false, updatable = false)
    var receiverId: IdType, // Store receiver ID directly
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    var type: NotificationType,

    // Store raw data needed for rendering, client-side routing, and aggregation
    // e.g., {"actorId": 5, "postId": 10, "projectId": 1, "actorUsername": "Alice"}
    @Type(JsonBinaryType::class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    var metadata: String? = null, // Make it nullable, store JSON string
    @Column(nullable = false) var read: Boolean = false,

    // --- Aggregation Fields ---
    @Column(nullable = false)
    var isAggregatable: Boolean = false, // Is this notification part of an active aggregation?
    @Column(name = "aggregation_key", length = 255)
    var aggregationKey: String? = null, // Key for grouping (e.g., "REACTION:POST:123")
    @Column(name = "aggregate_until")
    var aggregateUntil: Instant? = null, // Timestamp until which aggregation is active
    @Column(nullable = false)
    var finalized: Boolean = true, // True if ready/sent, False if waiting for aggregation job
    @Version // For optimistic locking
    var version: Long = 0,
) : BaseEntity() {
    fun isAggregationActive(): Boolean {
        return isAggregatable && !finalized && aggregateUntil?.isAfter(Instant.now()) ?: false
    }
}

fun NotificationType.toDTO(): NotificationTypeDTO {
    return NotificationTypeDTO.forValue(this.name)
}

fun NotificationTypeDTO.toEnum(): NotificationType {
    return NotificationType.valueOf(this.value)
}
