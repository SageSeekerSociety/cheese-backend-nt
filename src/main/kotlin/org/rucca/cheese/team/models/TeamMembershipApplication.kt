package org.rucca.cheese.team.models

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.model.ApplicationStatusDTO
import org.rucca.cheese.model.ApplicationTypeDTO
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamMemberRole
import org.rucca.cheese.user.User

/**
 * Enum defining the type of the membership application.
 *
 * REQUEST: User initiated request to join the team. INVITATION: Team initiated invitation for a
 * user to join.
 */
enum class ApplicationType {
    REQUEST,
    INVITATION,
}

fun ApplicationType.toDTO(): ApplicationTypeDTO {
    return when (this) {
        ApplicationType.REQUEST -> ApplicationTypeDTO.REQUEST
        ApplicationType.INVITATION -> ApplicationTypeDTO.INVITATION
    }
}

fun ApplicationTypeDTO.toEnum(): ApplicationType {
    return when (this) {
        ApplicationTypeDTO.REQUEST -> ApplicationType.REQUEST
        ApplicationTypeDTO.INVITATION -> ApplicationType.INVITATION
    }
}

/**
 * Enum defining the status of the membership application.
 *
 * PENDING: Waiting for action (approval for REQUEST, acceptance for INVITATION). APPROVED: Request
 * approved by team admin/owner. REJECTED: Request rejected by team admin/owner. ACCEPTED:
 * Invitation accepted by the user. DECLINED: Invitation declined by the user. CANCELED: Application
 * canceled by the initiator before completion. EXPIRED: Application expired due to inactivity
 * (optional).
 */
enum class ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ACCEPTED,
    DECLINED,
    CANCELED,
    // EXPIRED // Optional status
}

fun ApplicationStatus.toDTO(): ApplicationStatusDTO {
    return when (this) {
        ApplicationStatus.PENDING -> ApplicationStatusDTO.PENDING
        ApplicationStatus.APPROVED -> ApplicationStatusDTO.APPROVED
        ApplicationStatus.REJECTED -> ApplicationStatusDTO.REJECTED
        ApplicationStatus.ACCEPTED -> ApplicationStatusDTO.ACCEPTED
        ApplicationStatus.DECLINED -> ApplicationStatusDTO.DECLINED
        ApplicationStatus.CANCELED -> ApplicationStatusDTO.CANCELED
    // ApplicationStatus.EXPIRED -> ApplicationStatusDTO.EXPIRED // Optional status
    }
}

fun ApplicationStatusDTO.toEnum(): ApplicationStatus {
    return when (this) {
        ApplicationStatusDTO.PENDING -> ApplicationStatus.PENDING
        ApplicationStatusDTO.APPROVED -> ApplicationStatus.APPROVED
        ApplicationStatusDTO.REJECTED -> ApplicationStatus.REJECTED
        ApplicationStatusDTO.ACCEPTED -> ApplicationStatus.ACCEPTED
        ApplicationStatusDTO.DECLINED -> ApplicationStatus.DECLINED
        ApplicationStatusDTO.CANCELED -> ApplicationStatus.CANCELED
    // ApplicationStatusDTO.EXPIRED -> ApplicationStatus.EXPIRED // Optional status
    }
}

/**
 * Entity representing a user's request to join a team or a team's invitation to a user. This entity
 * manages the workflow before a final TeamUserRelation is established.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(
                name = "idx_team_membership_application_team_user_status",
                columnList = "team_id, user_id, status",
            ), // Efficiently query applications by team, user, and status
            Index(
                name = "idx_team_membership_application_user_type_status",
                columnList = "user_id, type, status",
            ), // Efficiently query user's pending requests/invitations
            Index(
                name = "idx_team_membership_application_team_type_status",
                columnList = "team_id, type, status",
            ), // Efficiently query team's pending requests/invitations
        ]
)
class TeamMembershipApplication(

    // The user who is the subject of the application (either applying or being invited).
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val user: User,

    // The team involved in the application.
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val team: Team,

    // The user who initiated this application (the user themselves for REQUEST, team admin/owner
    // for INVITATION).
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) val initiator: User,

    // Type of the application: user request or team invitation.
    @Column(nullable = false) @Enumerated(EnumType.STRING) val type: ApplicationType,

    // Current status of the application.
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ApplicationStatus = ApplicationStatus.PENDING,

    // The role requested (for REQUEST type, often MEMBER) or offered (for INVITATION type).
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val role: TeamMemberRole = TeamMemberRole.MEMBER, // Default role, can be set during creation

    // Optional message from the initiator (e.g., reason for request, invitation message).
    @Column(columnDefinition = "TEXT") var message: String? = null,

    // The user who processed the application (approved, rejected, accepted, declined).
    @JoinColumn(nullable = true) // Null until processed
    @ManyToOne(fetch = FetchType.LAZY)
    var processedBy: User? = null,

    // Timestamp when the application was processed.
    @Column(nullable = true) // Null until processed
    var processedAt: OffsetDateTime? = null,
) : BaseEntity()
