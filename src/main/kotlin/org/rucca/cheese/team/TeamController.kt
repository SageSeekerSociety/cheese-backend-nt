/*
 *  Description: This file defines the TeamController class.
 *               It provides endpoints of /teams
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.team

import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.api.TeamsApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.AuthContext
import org.rucca.cheese.auth.spring.AuthUser
import org.rucca.cheese.auth.spring.ResourceId
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.model.*
import org.rucca.cheese.team.models.toEnum
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseNewAuth
class TeamController(
    private val teamService: TeamService,
    private val jwtService: JwtService,
    private val teamMembershipService: TeamMembershipService,
) : TeamsApi {
    private val logger = LoggerFactory.getLogger(TeamController::class.java)

    @Auth("team:delete:team") // Requires OWNER role
    override suspend fun deleteTeam(@ResourceId teamId: Long): ResponseEntity<Unit> {
        teamService.deleteTeam(teamId)
        return ResponseEntity.noContent().build()
    }

    @Auth(
        "team:remove_member:membership"
    ) // Permission check (e.g., MEMBER can remove self, ADMIN/OWNER others)
    override suspend fun deleteTeamMember(
        @AuthContext("teamId") teamId: Long,
        @AuthContext("targetUserId") userId: Long,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        // Service needs to internally get the initiator ID via authenticateService
        teamService.removeTeamMember(teamId, userId /* initiatorId resolved in service */)
        val teamDTO = teamService.getTeamDto(teamId, jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Auth("team:view:team") // Requires MEMBER role or higher
    override suspend fun getTeam(@ResourceId teamId: Long): ResponseEntity<GetTeam200ResponseDTO> {
        val teamDTO = teamService.getTeamDto(teamId, jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Auth("team:enumerate:team") // Requires SystemRole.USER (login)
    override suspend fun getTeams(
        query: String,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<GetTeams200ResponseDTO> {
        val (teamDTOs, page) =
            teamService.enumerateTeams(jwtService.getCurrentUserId(), query, pageStart, pageSize)
        return ResponseEntity.ok(
            GetTeams200ResponseDTO(200, GetTeams200ResponseDataDTO(teamDTOs, page), "OK")
        )
    }

    @Auth // Login check only
    override suspend fun getMyTeams(): ResponseEntity<GetMyTeams200ResponseDTO> {
        // Service needs to internally get the current user ID via authenticateService
        val teamDTOs = teamService.getTeamsOfUser(jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetMyTeams200ResponseDTO(200, GetMyTeams200ResponseDataDTO(teamDTOs), "OK")
        )
    }

    @Auth("team:view:membership") // Requires MEMBER role or higher
    override suspend fun getTeamMembers(
        @ResourceId @AuthContext("teamId") teamId: Long,
        queryRealNameStatus: Boolean,
    ): ResponseEntity<GetTeamMembers200ResponseDTO> {
        val (memberDTOs, allMembersVerified) =
            teamService.getTeamMembers(teamId, queryRealNameStatus)
        return ResponseEntity.ok(
            GetTeamMembers200ResponseDTO(
                200,
                GetTeamMembers200ResponseDataDTO(
                    members = memberDTOs,
                    allMembersVerified = allMembersVerified,
                ),
                "OK",
            )
        )
    }

    @Auth("team:update:team") // Requires ADMIN role or higher
    override suspend fun patchTeam(
        @ResourceId teamId: Long,
        patchTeamRequestDTO: PatchTeamRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        if (patchTeamRequestDTO.name != null)
            teamService.updateTeamName(teamId, patchTeamRequestDTO.name)
        if (patchTeamRequestDTO.intro != null)
            teamService.updateTeamIntro(teamId, patchTeamRequestDTO.intro)
        if (patchTeamRequestDTO.description != null)
            teamService.updateTeamDescription(teamId, patchTeamRequestDTO.description)
        if (patchTeamRequestDTO.avatarId != null)
            teamService.updateTeamAvatar(teamId, patchTeamRequestDTO.avatarId)
        val teamDTO = teamService.getTeamDto(teamId, jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    // Endpoint for updating an existing member's role
    @Auth("team:update_member_role:membership") // Requires ADMIN/OWNER role
    override suspend fun patchTeamMember(
        @AuthContext("teamId") teamId: Long,
        @AuthContext("targetUserId") userId: Long,
        patchTeamMemberRequestDTO: PatchTeamMemberRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        val newRoleDto =
            patchTeamMemberRequestDTO.role
                ?: throw BadRequestError("Missing 'role' in request body for patchTeamMember.")

        val newRole =
            when (newRoleDto) {
                TeamMemberRoleTypeDTO.ADMIN -> TeamMemberRole.ADMIN
                TeamMemberRoleTypeDTO.MEMBER -> TeamMemberRole.MEMBER
                TeamMemberRoleTypeDTO.OWNER ->
                    throw BadRequestError("Cannot set role to OWNER using this endpoint.")
            }
        teamService.updateTeamMemberRole(
            teamId,
            userId,
            newRole, /* initiatorId resolved in service */
        )
        val teamDTO = teamService.getTeamDto(teamId, jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Auth("team:create:team") // Requires SystemRole.USER (login)
    override suspend fun postTeam(
        postTeamRequestDTO: PostTeamRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        val teamId =
            teamService.createTeam(
                name = postTeamRequestDTO.name,
                intro = postTeamRequestDTO.intro,
                description = postTeamRequestDTO.description,
                avatarId = postTeamRequestDTO.avatarId,
                ownerId = jwtService.getCurrentUserId(),
            )
        val teamDTO = teamService.getTeamDto(teamId, jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Auth
    override suspend fun postTeamMember(
        @ResourceId teamId: Long,
        postTeamMemberRequestDTO: PostTeamMemberRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        logger.warn("Endpoint POST /teams/{}/members is deprecated. Use invitations.", teamId)
        return ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED)
    }

    @Auth("team:approve_request:request")
    override suspend fun approveTeamJoinRequest(
        @AuthContext("teamId") teamId: Long,
        @ResourceId requestId: Long,
    ): ResponseEntity<Unit> {
        val currentUserId = jwtService.getCurrentUserId()
        withContext(Dispatchers.IO) {
            teamMembershipService.approveTeamJoinRequest(currentUserId, teamId, requestId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth("team:cancel_invitation:invitation")
    override suspend fun cancelTeamInvitation(
        @AuthContext("teamId") teamId: Long,
        @ResourceId invitationId: Long,
    ): ResponseEntity<Unit> {
        val currentUserId = jwtService.getCurrentUserId()
        withContext(Dispatchers.IO) {
            teamMembershipService.cancelTeamInvitation(currentUserId, teamId, invitationId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth("team:create_invitation:invitation")
    override suspend fun createTeamInvitation(
        @AuthContext("teamId") teamId: Long,
        teamInvitationCreateDTO: TeamInvitationCreateDTO,
    ): ResponseEntity<CreateTeamInvitation201ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val applicationDto =
            withContext(Dispatchers.IO) {
                teamMembershipService.createTeamInvitation(
                    initiatorUserId = currentUserId,
                    teamId = teamId,
                    userIdToInvite = teamInvitationCreateDTO.userId,
                    role = teamInvitationCreateDTO.role.toTeamMemberRole(),
                    message = teamInvitationCreateDTO.message,
                    // Service internally gets initiator ID
                )
            }
        val responseData = CreateTeamInvitation201ResponseDataDTO(invitation = applicationDto)
        val responseDto =
            CreateTeamInvitation201ResponseDTO(
                code = 201,
                data = responseData,
                message = "Invitation created successfully",
            )
        return ResponseEntity.created(URI.create("/users/me/team-invitations/${applicationDto.id}"))
            .body(responseDto)
    }

    @Auth
    override suspend fun createTeamJoinRequest(
        @AuthUser userInfo: AuthUserInfo?,
        @AuthContext("teamId") teamId: Long,
        teamJoinRequestCreateDTO: TeamJoinRequestCreateDTO?,
    ): ResponseEntity<CreateTeamJoinRequest201ResponseDTO> {
        val applicationDto =
            withContext(Dispatchers.IO) {
                teamMembershipService.createTeamJoinRequest(
                    userId = userInfo!!.userId,
                    teamId = teamId,
                    message = teamJoinRequestCreateDTO?.message,
                )
            }
        val responseData = CreateTeamJoinRequest201ResponseDataDTO(application = applicationDto)
        val responseDto =
            CreateTeamJoinRequest201ResponseDTO(
                code = 201,
                data = responseData,
                message = "Request created successfully",
            )
        return ResponseEntity.created(URI.create("/users/team-requests/${applicationDto.id}"))
            .body(responseDto)
    }

    @Auth("team:view:invitation")
    override suspend fun listTeamInvitations(
        @AuthContext("teamId") teamId: Long,
        status: ApplicationStatusDTO?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ListTeamInvitations200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val (invitations, pageDto) =
            teamMembershipService.listTeamInvitations(
                requestingUserId = currentUserId,
                teamId = teamId,
                status = status?.toEnum(),
                cursorId = pageStart,
                pageSize = pageSize,
            )
        return ResponseEntity.ok(
            ListTeamInvitations200ResponseDTO(
                code = 200,
                data =
                    ListMyInvitations200ResponseDataDTO(invitations = invitations, page = pageDto),
                message = "OK",
            )
        )
    }

    @Auth("team:view:request")
    override suspend fun listTeamJoinRequests(
        @AuthContext("teamId") teamId: Long,
        status: ApplicationStatusDTO?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ListTeamJoinRequests200ResponseDTO> {
        val currentUserId = jwtService.getCurrentUserId()
        val (applications, pageDto) =
            teamMembershipService.listTeamJoinRequests(
                requestingUserId = currentUserId,
                teamId = teamId,
                status = status?.toEnum(),
                cursorId = pageStart,
                pageSize = pageSize,
            )
        return ResponseEntity.ok(
            ListTeamJoinRequests200ResponseDTO(
                code = 200,
                data =
                    ListTeamJoinRequests200ResponseDataDTO(
                        applications = applications,
                        page = pageDto,
                    ),
                message = "OK",
            )
        )
    }

    @Auth("team:reject_request:request")
    override suspend fun rejectTeamJoinRequest(
        @AuthContext("teamId") teamId: Long,
        @ResourceId requestId: Long,
    ): ResponseEntity<Unit> {
        val currentUserId = jwtService.getCurrentUserId()
        withContext(Dispatchers.IO) {
            teamMembershipService.rejectTeamJoinRequest(currentUserId, teamId, requestId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun acceptTeamInvitation(
        @AuthUser userInfo: AuthUserInfo?,
        invitationId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamMembershipService.acceptTeamInvitation(userInfo!!.userId, invitationId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun cancelMyJoinRequest(
        @AuthUser userInfo: AuthUserInfo?,
        requestId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamMembershipService.cancelMyJoinRequest(userInfo!!.userId, requestId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun declineTeamInvitation(
        @AuthUser userInfo: AuthUserInfo?,
        invitationId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamMembershipService.declineTeamInvitation(userInfo!!.userId, invitationId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth
    override suspend fun listMyInvitations(
        @AuthUser userInfo: AuthUserInfo?,
        status: ApplicationStatusDTO?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ListMyInvitations200ResponseDTO> {
        val (invitations, pageDto) =
            teamMembershipService.listMyInvitations(
                userId = userInfo!!.userId,
                status = status?.toEnum(),
                cursorId = pageStart,
                pageSize = pageSize.coerceIn(1, 100),
            )
        return ResponseEntity.ok(
            ListMyInvitations200ResponseDTO(
                code = 200,
                data = ListMyInvitations200ResponseDataDTO(invitations, pageDto),
                message = "Success",
            )
        )
    }

    @Auth
    override suspend fun listMyJoinRequests(
        @AuthUser userInfo: AuthUserInfo?,
        status: ApplicationStatusDTO?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ListMyJoinRequests200ResponseDTO> {
        val (requests, pageDto) =
            teamMembershipService.listMyJoinRequests(
                userId = userInfo!!.userId,
                status = status?.toEnum(),
                cursorId = pageStart,
                pageSize = pageSize.coerceIn(1, 100),
            )
        return ResponseEntity.ok(
            ListMyJoinRequests200ResponseDTO(
                code = 200,
                data = ListMyJoinRequests200ResponseDataDTO(requests, pageDto),
                message = "Success",
            )
        )
    }

    @Auth("team:remove_member:membership")
    override suspend fun leaveTeam(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId teamId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            teamService.removeTeamMember(teamId, userInfo!!.userId, userInfo.userId)
        }
        return ResponseEntity.noContent().build()
    }
}
