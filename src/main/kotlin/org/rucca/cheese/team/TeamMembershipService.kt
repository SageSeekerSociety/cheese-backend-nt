package org.rucca.cheese.team

import java.time.OffsetDateTime
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.SimpleCursor
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.spec.CursorSpecificationBuilder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.TeamMembershipApplicationDTO
import org.rucca.cheese.notification.event.NotificationTriggerEvent
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.team.error.PendingApplicationExistsError
import org.rucca.cheese.team.error.UserAlreadyMemberError
import org.rucca.cheese.team.models.ApplicationStatus
import org.rucca.cheese.team.models.ApplicationType
import org.rucca.cheese.team.models.TeamMembershipApplication
import org.rucca.cheese.team.models.toDTO
import org.rucca.cheese.team.repositories.TeamMembershipApplicationRepository
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserRepository
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

fun TeamMembershipApplication.toDTO(
    userService: UserService,
    teamService: TeamService,
): TeamMembershipApplicationDTO {
    val userDto = userService.getUserDto(this.user.id!!.toLong())
    val teamSummaryDto = teamService.getTeamSummaryDTO(this.team.id!!)
    val initiatorDto = userService.getUserDto(this.initiator.id!!.toLong())
    val processedByDto = this.processedBy?.id?.let { userService.getUserDto(it.toLong()) }

    return TeamMembershipApplicationDTO(
        id = this.id!!,
        user = userDto,
        team = teamSummaryDto,
        initiator = initiatorDto,
        type = this.type.toDTO(),
        status = this.status.toDTO(),
        role = teamService.convertMemberRole(this.role),
        message = this.message,
        processedBy = processedByDto,
        processedAt = this.processedAt?.toEpochMilli(),
        createdAt = this.createdAt.toEpochMilli(),
        updatedAt = this.updatedAt.toEpochMilli(),
    )
}

@Service
class TeamMembershipService(
    private val applicationRepository: TeamMembershipApplicationRepository,
    private val teamService: TeamService,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TeamMembershipService::class.java)

        private const val DEFAULT_PAGE_SIZE = 20
        private val DEFAULT_SORT_DIRECTION = Sort.Direction.DESC
        private val ID_PROPERTY_PATH = "id"
    }

    private fun getApplicationById(applicationId: IdType): TeamMembershipApplication {
        return applicationRepository.findById(applicationId).orElseThrow {
            NotFoundError("TeamMembershipApplication", applicationId)
        }
    }

    private fun validateUserCanApplyOrBeInvited(userId: IdType, teamId: IdType) {
        if (teamService.isTeamMember(teamId, userId)) {
            throw UserAlreadyMemberError(teamId, userId)
        }
        if (
            applicationRepository.existsByUserIdAndTeamIdAndStatus(
                userId,
                teamId,
                ApplicationStatus.PENDING,
            )
        ) {
            throw PendingApplicationExistsError(teamId, userId)
        }
    }

    private fun updateApplicationStatus(
        application: TeamMembershipApplication,
        newStatus: ApplicationStatus,
        processorId: IdType,
    ): TeamMembershipApplication {
        application.status = newStatus
        application.processedBy = userRepository.getReferenceById(processorId.toInt())
        application.processedAt = OffsetDateTime.now()
        return applicationRepository.save(application)
    }

    @Transactional
    fun createTeamJoinRequest(
        userId: IdType,
        teamId: IdType,
        message: String?,
    ): TeamMembershipApplicationDTO {
        val user = userService.getUserReference(userId)
        val team = teamService.getTeamReference(teamId)

        validateUserCanApplyOrBeInvited(userId, teamId)

        val application =
            TeamMembershipApplication(
                user = user,
                team = team,
                initiator = user, // User initiates their own request
                type = ApplicationType.REQUEST,
                status = ApplicationStatus.PENDING,
                role = TeamMemberRole.MEMBER,
                message = message,
            )
        val savedApp = applicationRepository.save(application)

        val adminAndOwnerIds = teamService.getTeamAdminAndOwnerIds(teamId)
        if (adminAndOwnerIds.isNotEmpty()) {
            val payload =
                mapOf(
                    "requester" to createEntityReference("user", userId),
                    "team" to createEntityReference("team", teamId),
                    "application" to
                        createEntityReference("team_membership_application", savedApp.id!!),
                    "message" to (message ?: ""),
                )
            publishNotificationEvent(
                recipientIds = adminAndOwnerIds,
                type = NotificationType.TEAM_JOIN_REQUEST,
                payload = payload,
                actorId = userId,
            )
        } else {
            log.warn(
                "No admins or owners found for team {} to notify about join request {}",
                teamId,
                savedApp.id,
            )
        }

        return savedApp.toDTO(userService, teamService)
    }

    @Transactional
    fun cancelMyJoinRequest(userId: IdType, requestId: IdType) {
        val application =
            applicationRepository
                .findByIdAndInitiatorIdAndTypeAndStatus(
                    id = requestId,
                    initiatorId = userId,
                    type = ApplicationType.REQUEST,
                    status = ApplicationStatus.PENDING,
                )
                .orElseThrow { NotFoundError("Pending request initiated by user", requestId) }

        updateApplicationStatus(application, ApplicationStatus.CANCELED, userId)
    }

    fun listMyJoinRequests(
        userId: IdType,
        status: ApplicationStatus?,
        cursorId: IdType?,
        pageSize: Int?,
    ): Pair<List<TeamMembershipApplicationDTO>, PageDTO> {
        val effectivePageSize = pageSize ?: DEFAULT_PAGE_SIZE
        val cursor = cursorId?.let { SimpleCursor<TeamMembershipApplication, IdType>(it) }

        val specBuilder =
            CursorSpecificationBuilder.simple(TeamMembershipApplication::id)
                .sortByPath(ID_PROPERTY_PATH, DEFAULT_SORT_DIRECTION)
                .cursorByPath(ID_PROPERTY_PATH)
                .specification { root, _, cb ->
                    val predicates =
                        mutableListOf(
                            cb.equal(root.get<User>("user").get<IdType>("id"), userId),
                            cb.equal(root.get<ApplicationType>("type"), ApplicationType.REQUEST),
                        )
                    status?.let {
                        predicates.add(cb.equal(root.get<ApplicationStatus>("status"), it))
                    }
                    cb.and(*predicates.toTypedArray())
                }

        val page =
            applicationRepository.findAllWithCursor(specBuilder.build(), cursor, effectivePageSize)

        val dtoList = page.content.map { it.toDTO(userService, teamService) }
        return Pair(dtoList, page.pageInfo.toPageDTO())
    }

    @Transactional
    fun createTeamInvitation(
        initiatorUserId: IdType,
        teamId: IdType,
        userIdToInvite: IdType,
        role: TeamMemberRole?,
        message: String?,
    ): TeamMembershipApplicationDTO {
        if (!teamService.isTeamAtLeastAdmin(teamId, initiatorUserId)) {
            throw ForbiddenError(
                "User $initiatorUserId is not authorized to invite members to team $teamId."
            )
        }

        val invitedUser = userService.getUserReference(userIdToInvite)
        val team = teamService.getTeamReference(teamId)
        val initiator = userService.getUserReference(initiatorUserId)

        validateUserCanApplyOrBeInvited(userIdToInvite, teamId)

        val application =
            TeamMembershipApplication(
                user = invitedUser,
                team = team,
                initiator = initiator, // Admin/Owner initiates
                type = ApplicationType.INVITATION,
                status = ApplicationStatus.PENDING,
                role = role ?: TeamMemberRole.MEMBER,
                message = message,
            )
        val savedApp = applicationRepository.save(application)

        val payload =
            mapOf(
                "inviter" to createEntityReference("user", initiatorUserId),
                "invitedUser" to createEntityReference("user", userIdToInvite),
                "team" to createEntityReference("team", teamId),
                "application" to
                    createEntityReference("team_membership_application", savedApp.id!!),
                "role" to savedApp.role.name,
                "message" to (message ?: ""),
            )
        publishNotificationEvent(
            recipientIds = setOf(userIdToInvite), // Notify the invited user
            type = NotificationType.TEAM_INVITATION,
            payload = payload,
            actorId = initiatorUserId,
        )

        return savedApp.toDTO(userService, teamService)
    }

    @Transactional
    fun acceptTeamInvitation(userId: IdType, invitationId: IdType) {
        val application =
            applicationRepository
                .findByIdAndUserIdAndTypeAndStatus(
                    id = invitationId,
                    userId = userId,
                    type = ApplicationType.INVITATION,
                    status = ApplicationStatus.PENDING,
                )
                .orElseThrow { NotFoundError("Pending invitation for user", invitationId) }

        if (teamService.isTeamMember(application.team.id!!, userId)) {
            throw UserAlreadyMemberError(
                application.team.id!!,
                userId,
                "Cannot accept invitation, user is already a member.",
            )
        }

        val initiatorId = application.initiator.id!!.toLong()
        val teamId = application.team.id!!

        updateApplicationStatus(application, ApplicationStatus.ACCEPTED, userId)
        teamService.createMembershipFromApplication(application)

        val payload =
            mapOf(
                "accepter" to createEntityReference("user", userId),
                "team" to createEntityReference("team", teamId),
                "application" to createEntityReference("team_membership_application", invitationId),
                "inviter" to createEntityReference("user", initiatorId),
            )
        publishNotificationEvent(
            recipientIds = setOf(initiatorId), // Notify the original inviter
            type = NotificationType.TEAM_INVITATION_ACCEPTED,
            payload = payload,
            actorId = userId,
        )
    }

    @Transactional
    fun declineTeamInvitation(userId: IdType, invitationId: IdType) {
        val application =
            applicationRepository
                .findByIdAndUserIdAndTypeAndStatus(
                    id = invitationId,
                    userId = userId,
                    type = ApplicationType.INVITATION,
                    status = ApplicationStatus.PENDING,
                )
                .orElseThrow { NotFoundError("Pending invitation for user", invitationId) }

        val initiatorId = application.initiator.id!!.toLong()
        val teamId = application.team.id!!

        updateApplicationStatus(application, ApplicationStatus.DECLINED, userId)

        val payload =
            mapOf(
                "decliner" to createEntityReference("user", userId),
                "team" to createEntityReference("team", teamId),
                "application" to createEntityReference("team_membership_application", invitationId),
                "inviter" to createEntityReference("user", initiatorId),
            )
        publishNotificationEvent(
            recipientIds = setOf(initiatorId), // Notify the original inviter
            type = NotificationType.TEAM_INVITATION_DECLINED,
            payload = payload,
            actorId = userId,
        )
    }

    fun listMyInvitations(
        userId: IdType, // Added userId parameter
        status: ApplicationStatus?,
        cursorId: IdType?,
        pageSize: Int?,
    ): Pair<List<TeamMembershipApplicationDTO>, PageDTO> {
        val effectivePageSize = pageSize ?: DEFAULT_PAGE_SIZE
        val cursor = cursorId?.let { SimpleCursor<TeamMembershipApplication, IdType>(it) }

        val specBuilder =
            CursorSpecificationBuilder.simple<TeamMembershipApplication, IdType>(
                    TeamMembershipApplication::id
                )
                .sortByPath(ID_PROPERTY_PATH, DEFAULT_SORT_DIRECTION)
                .cursorByPath(ID_PROPERTY_PATH)
                .specification { root, _, cb ->
                    val predicates =
                        mutableListOf(
                            cb.equal(
                                root.get<User>("user").get<IdType>("id"),
                                userId,
                            ), // Use passed userId
                            cb.equal(root.get<ApplicationType>("type"), ApplicationType.INVITATION),
                        )
                    status?.let {
                        predicates.add(cb.equal(root.get<ApplicationStatus>("status"), it))
                    }
                    cb.and(*predicates.toTypedArray())
                }

        val page =
            applicationRepository.findAllWithCursor(specBuilder.build(), cursor, effectivePageSize)
        val dtoList = page.content.map { it.toDTO(userService, teamService) }
        return Pair(dtoList, page.pageInfo.toPageDTO())
    }

    @Transactional
    fun approveTeamJoinRequest(approverUserId: IdType, teamId: IdType, requestId: IdType) {
        if (!teamService.isTeamAtLeastAdmin(teamId, approverUserId)) {
            throw ForbiddenError(
                "User $approverUserId is not authorized to approve requests for team $teamId."
            )
        }

        val application =
            applicationRepository
                .findByIdAndTeamIdAndTypeAndStatus(
                    id = requestId,
                    teamId = teamId,
                    type = ApplicationType.REQUEST,
                    status = ApplicationStatus.PENDING,
                )
                .orElseThrow { NotFoundError("Pending request for team", requestId) }

        val requesterId = application.user.id!!.toLong()

        if (teamService.isTeamMember(teamId, requesterId)) {
            throw UserAlreadyMemberError(
                teamId,
                requesterId,
                "Cannot approve request, user is already a member.",
            )
        }

        updateApplicationStatus(application, ApplicationStatus.APPROVED, approverUserId)
        teamService.createMembershipFromApplication(application)

        val payload =
            mapOf(
                "approver" to createEntityReference("user", approverUserId),
                "team" to createEntityReference("team", teamId),
                "application" to createEntityReference("team_membership_application", requestId),
                "requester" to createEntityReference("user", requesterId),
            )
        publishNotificationEvent(
            recipientIds = setOf(requesterId), // Notify the original requester
            type = NotificationType.TEAM_REQUEST_APPROVED,
            payload = payload,
            actorId = approverUserId,
        )
    }

    @Transactional
    fun rejectTeamJoinRequest(rejectorUserId: IdType, teamId: IdType, requestId: IdType) {
        if (!teamService.isTeamAtLeastAdmin(teamId, rejectorUserId)) {
            throw ForbiddenError(
                "User $rejectorUserId is not authorized to reject requests for team $teamId."
            )
        }

        val application =
            applicationRepository
                .findByIdAndTeamIdAndTypeAndStatus(
                    id = requestId,
                    teamId = teamId,
                    type = ApplicationType.REQUEST,
                    status = ApplicationStatus.PENDING,
                )
                .orElseThrow { NotFoundError("Pending request for team", requestId) }

        val requesterId = application.user.id!!.toLong()

        // Update status using rejectorUserId
        updateApplicationStatus(application, ApplicationStatus.REJECTED, rejectorUserId)

        val payload =
            mapOf(
                "rejector" to createEntityReference("user", rejectorUserId),
                "team" to createEntityReference("team", teamId),
                "application" to createEntityReference("team_membership_application", requestId),
                "requester" to createEntityReference("user", requesterId),
            )
        publishNotificationEvent(
            recipientIds = setOf(requesterId), // Notify the original requester
            type = NotificationType.TEAM_REQUEST_REJECTED,
            payload = payload,
            actorId = rejectorUserId,
        )
    }

    @Transactional
    fun cancelTeamInvitation(cancelerUserId: IdType, teamId: IdType, invitationId: IdType) {
        if (!teamService.isTeamAtLeastAdmin(teamId, cancelerUserId)) {
            throw ForbiddenError(
                "User $cancelerUserId is not authorized to cancel invitations for team $teamId."
            )
        }

        val application =
            applicationRepository
                .findByIdAndTeamIdAndTypeAndStatus(
                    id = invitationId,
                    teamId = teamId,
                    type = ApplicationType.INVITATION,
                    status = ApplicationStatus.PENDING,
                )
                .orElseThrow { NotFoundError("Pending invitation for team", invitationId) }

        val invitedUserId = application.user.id!!.toLong()

        updateApplicationStatus(application, ApplicationStatus.CANCELED, cancelerUserId)

        val payload =
            mapOf(
                "canceler" to createEntityReference("user", cancelerUserId),
                "team" to createEntityReference("team", teamId),
                "application" to createEntityReference("team_membership_application", invitationId),
                "invitedUser" to createEntityReference("user", invitedUserId),
            )
        publishNotificationEvent(
            recipientIds = setOf(invitedUserId), // Notify the invited user
            type = NotificationType.TEAM_INVITATION_CANCELED,
            payload = payload,
            actorId = cancelerUserId,
        )
    }

    fun listTeamJoinRequests(
        requestingUserId: IdType, // Added requestingUserId parameter (for permission check)
        teamId: IdType,
        status: ApplicationStatus?,
        cursorId: IdType?,
        pageSize: Int?,
    ): Pair<List<TeamMembershipApplicationDTO>, PageDTO> {
        // Check permissions using requestingUserId
        if (!teamService.isTeamAtLeastAdmin(teamId, requestingUserId)) {
            throw ForbiddenError(
                "User $requestingUserId is not authorized to view requests for team $teamId."
            )
        }

        val effectivePageSize = pageSize ?: DEFAULT_PAGE_SIZE
        val cursor = cursorId?.let { SimpleCursor<TeamMembershipApplication, IdType>(it) }

        val specBuilder =
            CursorSpecificationBuilder.simple<TeamMembershipApplication, IdType>(
                    TeamMembershipApplication::id
                )
                .sortByPath(ID_PROPERTY_PATH, DEFAULT_SORT_DIRECTION)
                .cursorByPath(ID_PROPERTY_PATH)
                .specification { root, _, cb ->
                    val predicates =
                        mutableListOf(
                            cb.equal(
                                root
                                    .get<Team>("team")
                                    .get<IdType>("id"), // Adjusted Team model path if necessary
                                teamId,
                            ),
                            cb.equal(root.get<ApplicationType>("type"), ApplicationType.REQUEST),
                        )
                    status?.let {
                        predicates.add(cb.equal(root.get<ApplicationStatus>("status"), it))
                    }
                    cb.and(*predicates.toTypedArray())
                }

        val page =
            applicationRepository.findAllWithCursor(specBuilder.build(), cursor, effectivePageSize)
        val dtoList = page.content.map { it.toDTO(userService, teamService) }
        return Pair(dtoList, page.pageInfo.toPageDTO())
    }

    fun listTeamInvitations(
        requestingUserId: IdType, // Added requestingUserId parameter (for permission check)
        teamId: IdType,
        status: ApplicationStatus?,
        cursorId: IdType?,
        pageSize: Int?,
    ): Pair<List<TeamMembershipApplicationDTO>, PageDTO> {
        // Check permissions using requestingUserId
        if (!teamService.isTeamAtLeastAdmin(teamId, requestingUserId)) {
            throw ForbiddenError(
                "User $requestingUserId is not authorized to view invitations for team $teamId."
            )
        }

        val effectivePageSize = pageSize ?: DEFAULT_PAGE_SIZE
        val cursor = cursorId?.let { SimpleCursor<TeamMembershipApplication, IdType>(it) }

        val specBuilder =
            CursorSpecificationBuilder.simple<TeamMembershipApplication, IdType>(
                    TeamMembershipApplication::id
                )
                .sortByPath(ID_PROPERTY_PATH, DEFAULT_SORT_DIRECTION)
                .cursorByPath(ID_PROPERTY_PATH)
                .specification { root, _, cb ->
                    val predicates =
                        mutableListOf(
                            cb.equal(
                                root
                                    .get<Team>("team")
                                    .get<IdType>("id"), // Adjusted Team model path if necessary
                                teamId,
                            ),
                            cb.equal(root.get<ApplicationType>("type"), ApplicationType.INVITATION),
                        )
                    status?.let {
                        predicates.add(cb.equal(root.get<ApplicationStatus>("status"), it))
                    }
                    cb.and(*predicates.toTypedArray())
                }

        val page =
            applicationRepository.findAllWithCursor(specBuilder.build(), cursor, effectivePageSize)
        val dtoList = page.content.map { it.toDTO(userService, teamService) }
        return Pair(dtoList, page.pageInfo.toPageDTO())
    }

    // --- Private Helper for Publishing Events ---
    private fun publishNotificationEvent(
        recipientIds: Set<Long>,
        type: NotificationType,
        payload: Map<String, Any>,
        actorId: Long? = null,
    ) {
        if (recipientIds.isEmpty()) {
            log.warn(
                "Attempted to publish notification type {} but recipient set was empty. Payload: {}",
                type,
                payload,
            )
            return
        }
        try {
            val event =
                NotificationTriggerEvent(
                    source = this,
                    recipientIds = recipientIds,
                    type = type,
                    payload = payload,
                    actorId = actorId,
                )
            eventPublisher.publishEvent(event)
            log.info(
                "Published notification event: type={}, recipients={}, actor={}",
                type,
                recipientIds,
                actorId,
            )
        } catch (e: Exception) {
            log.error("Failed to publish notification event type {}: {}", type, e.message, e)
        }
    }

    /** Creates the standard entity reference map expected by the resolver. */
    private fun createEntityReference(type: String, id: Long): Map<String, String> {
        return mapOf("type" to type, "id" to id.toString())
    }
}
