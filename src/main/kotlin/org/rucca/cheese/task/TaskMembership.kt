/*
 *  Description: This file defines the TaskMembership entity and its repository.
 *               It stores the information of a task's membership.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *      CH3COOH-JYR
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskParticipantRealNameInfoDTO

@Embeddable
data class TeamMemberRealNameInfo(
    @Column(name = "member_id", nullable = false) val memberId: IdType,
    // Unique identifier for this specific member *slot* within *this* team's participation snapshot
    @Column(name = "participant_member_uuid", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    val participantMemberUuid: UUID = UUID.randomUUID(),
    @Embedded val realNameInfo: RealNameInfo,
)

@Embeddable
class RealNameInfo(
    @Column(name = "real_name", nullable = true) val realName: String? = null,
    @Column(name = "student_id", nullable = true) val studentId: String? = null,
    @Column(name = "grade", nullable = true) val grade: String? = null,
    @Column(name = "major", nullable = true) val major: String? = null,
    @Column(name = "class_name", nullable = true) val className: String? = null,
    @Column(name = "encrypted", nullable = true, columnDefinition = "BOOLEAN DEFAULT false")
    val encrypted: Boolean = false,
)

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "task_id"),
            Index(columnList = "member_id"),
            Index(columnList = "completion_status"),
        ]
)
class TaskMembership(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val task: Task? = null,
    @Column(nullable = false) val memberId: IdType? = null,
    @Column(nullable = true) var deadline: LocalDateTime? = null,
    @Column(nullable = false) var approved: ApproveType? = null,
    @Column(nullable = true) var rejectReason: String? = null,
    @Column(name = "is_team", nullable = false) val isTeam: Boolean = false,

    // Unique anonymous identifier for this participation record (individual or team)
    // Generated once when the record is created.
    @Column(name = "participant_uuid", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    val participantUuid: UUID = UUID.randomUUID(),

    // For individual members
    @Embedded var realNameInfo: RealNameInfo? = null,

    // For team members
    @ElementCollection
    @CollectionTable(
        name = "task_membership_team_members",
        joinColumns = [JoinColumn(name = "task_membership_id")],
        indexes = [Index(columnList = "participant_member_uuid")],
    )
    val teamMembersRealNameInfo: MutableList<TeamMemberRealNameInfo> = mutableListOf(),
    @Column(name = "email", nullable = false) val email: String? = null,
    @Column(name = "phone", nullable = false) val phone: String? = null,
    @Column(name = "apply_reason", nullable = false) val applyReason: String? = null,
    @Column(name = "personal_advantage", nullable = false) val personalAdvantage: String? = null,
    @Column(name = "remark", nullable = false) val remark: String? = null,
    @Column(nullable = true) var encryptionKeyId: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(
        name = "completion_status",
        nullable = false,
        columnDefinition = "VARCHAR(50) default 'NOT_SUBMITTED'",
    )
    var completionStatus: TaskCompletionStatus = TaskCompletionStatus.NOT_SUBMITTED,
) : BaseEntity()

// Default empty RealNameInfo when real name is not required or not available
val DefaultRealNameInfo =
    RealNameInfo(
        realName = null, // Use null for non-required fields
        studentId = null,
        grade = null,
        major = null,
        className = null,
        encrypted = false, // Explicitly mark as not encrypted
    )

fun RealNameInfo.convert(): TaskParticipantRealNameInfoDTO {
    return TaskParticipantRealNameInfoDTO(
        realName = realName ?: "", // Convert null to empty string for DTO if needed
        studentId = studentId ?: "",
        grade = grade ?: "",
        major = major ?: "",
        className = className ?: "",
    )
}

fun TaskParticipantRealNameInfoDTO.convert(): RealNameInfo {
    // This conversion might not be fully accurate if original was null vs empty string
    return RealNameInfo(
        realName = realName.ifBlank { null },
        studentId = studentId.ifBlank { null },
        grade = grade.ifBlank { null },
        major = major.ifBlank { null },
        className = className.ifBlank { null },
        encrypted = false, // Assume DTOs are decrypted
    )
}
