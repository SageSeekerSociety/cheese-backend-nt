package org.rucca.cheese.team.repositories

import java.util.*
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.team.models.ApplicationStatus
import org.rucca.cheese.team.models.ApplicationType
import org.rucca.cheese.team.models.TeamMembershipApplication

interface TeamMembershipApplicationRepository :
    CursorPagingRepository<TeamMembershipApplication, IdType> {
    // Find a pending application for a specific user and team
    fun findByUserIdAndTeamIdAndStatus(
        userId: IdType,
        teamId: IdType,
        status: ApplicationStatus = ApplicationStatus.PENDING,
    ): Optional<TeamMembershipApplication>

    // Check if a pending application exists for a user and team
    fun existsByUserIdAndTeamIdAndStatus(
        userId: IdType,
        teamId: IdType,
        status: ApplicationStatus = ApplicationStatus.PENDING,
    ): Boolean

    // Find specific application by ID, ensuring it belongs to the correct team and has expected
    // type/status (for processing)
    fun findByIdAndTeamIdAndTypeAndStatus(
        id: IdType,
        teamId: IdType,
        type: ApplicationType,
        status: ApplicationStatus,
    ): Optional<TeamMembershipApplication>

    // Find specific application by ID, ensuring it belongs to the correct user and has expected
    // type/status (for processing)
    fun findByIdAndUserIdAndTypeAndStatus(
        id: IdType,
        userId: IdType,
        type: ApplicationType,
        status: ApplicationStatus,
    ): Optional<TeamMembershipApplication>

    // Find specific application by ID, ensuring it was initiated by the correct user and has
    // expected type/status (for cancellation)
    fun findByIdAndInitiatorIdAndTypeAndStatus(
        id: IdType,
        initiatorId: IdType,
        type: ApplicationType,
        status: ApplicationStatus,
    ): Optional<TeamMembershipApplication>
}
