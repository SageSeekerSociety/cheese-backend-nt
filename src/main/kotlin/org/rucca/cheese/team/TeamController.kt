/*
 *  Description: This file defines the TeamController class.
 *               It provides endpoints of /teams
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.team

import jakarta.annotation.PostConstruct
import org.rucca.cheese.api.TeamsApi
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.NoAuth
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.auth.spring.UseOldAuth
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseOldAuth
class TeamController(
    private val teamService: TeamService,
    private val authorizationService: AuthorizationService,
    private val jwtService: JwtService,
) : TeamsApi {
    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register("team", teamService::getTeamOwner)
        authorizationService.customAuthLogics.register("is-team-admin") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any?>?,
            _: IdGetter?,
            _: Any? ->
            teamService.isTeamAtLeastAdmin(
                resourceId ?: throw IllegalArgumentException("resourceId is null"),
                userId,
            )
        }
        authorizationService.customAuthLogics.register("member-is-self") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            authInfo: Map<String, Any?>?,
            _: IdGetter?,
            _: Any? ->
            userId == authInfo?.get("member")
        }
    }

    @Guard("delete", "team")
    override fun deleteTeam(@ResourceId teamId: Long): ResponseEntity<CommonResponseDTO> {
        teamService.deleteTeam(teamId)
        return ResponseEntity.ok(CommonResponseDTO(200, "OK"))
    }

    @NoAuth
    override fun deleteTeamMember(
        @ResourceId teamId: Long,
        userId: Long,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        val role = teamService.getTeamMemberRole(teamId, userId)
        when (role) {
            TeamMemberRoleTypeDTO.OWNER ->
                throw IllegalArgumentException("Cannot remove team owner")
            TeamMemberRoleTypeDTO.ADMIN -> {
                authorizationService.audit("remove-admin", "team", teamId)
                teamService.removeTeamMember(teamId, userId)
            }
            TeamMemberRoleTypeDTO.MEMBER -> {
                authorizationService.audit(
                    "remove-normal-member",
                    "team",
                    teamId,
                    mapOf("member" to userId),
                )
                teamService.removeTeamMember(teamId, userId)
            }
        }
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Guard("query", "team")
    override fun getTeam(@ResourceId teamId: Long): ResponseEntity<GetTeam200ResponseDTO> {
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Guard("enumerate", "team")
    override fun getTeams(
        query: String,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<GetTeams200ResponseDTO> {
        val (teamDTOs, page) = teamService.enumerateTeams(query, pageStart, pageSize)
        return ResponseEntity.ok(
            GetTeams200ResponseDTO(200, GetTeams200ResponseDataDTO(teamDTOs, page), "OK")
        )
    }

    @Guard("enumerate-my-teams", "team")
    override fun getMyTeams(): ResponseEntity<GetMyTeams200ResponseDTO> {
        val teamDTOs = teamService.getTeamsOfUser(jwtService.getCurrentUserId())
        return ResponseEntity.ok(
            GetMyTeams200ResponseDTO(200, GetMyTeams200ResponseDataDTO(teamDTOs), "OK")
        )
    }

    @Guard("enumerate-members", "team")
    override fun getTeamMembers(
        @ResourceId teamId: Long,
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

    @Guard("modify", "team")
    override fun patchTeam(
        @ResourceId teamId: Long,
        patchTeamRequestDTO: PatchTeamRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        if (patchTeamRequestDTO.name != null) {
            teamService.updateTeamName(teamId, patchTeamRequestDTO.name)
        }
        if (patchTeamRequestDTO.intro != null) {
            teamService.updateTeamIntro(teamId, patchTeamRequestDTO.intro)
        }
        if (patchTeamRequestDTO.description != null) {
            teamService.updateTeamDescription(teamId, patchTeamRequestDTO.description)
        }
        if (patchTeamRequestDTO.avatarId != null) {
            teamService.updateTeamAvatar(teamId, patchTeamRequestDTO.avatarId)
        }
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @NoAuth
    override fun patchTeamMember(
        @ResourceId teamId: Long,
        userId: Long,
        patchTeamMemberRequestDTO: PatchTeamMemberRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        if (patchTeamMemberRequestDTO.role != null) {
            when (patchTeamMemberRequestDTO.role) {
                TeamMemberRoleTypeDTO.OWNER -> {
                    authorizationService.audit("ship-ownership", "team", teamId)
                    teamService.removeTeamMember(teamId, userId)
                    teamService.shipTeamOwnershipToNoneMember(teamId, userId)
                }
                TeamMemberRoleTypeDTO.ADMIN -> {
                    authorizationService.audit("add-admin", "team", teamId)
                    teamService.removeTeamMember(teamId, userId)
                    teamService.addTeamAdmin(teamId, userId)
                }
                TeamMemberRoleTypeDTO.MEMBER -> {
                    authorizationService.audit(
                        "add-normal-member",
                        "team",
                        teamId,
                        mapOf("member" to userId),
                    )
                    teamService.removeTeamMember(teamId, userId)
                    teamService.addTeamNormalMember(teamId, userId)
                }
            }
        }
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @Guard("create", "team")
    override fun postTeam(
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
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }

    @NoAuth
    override fun postTeamMember(
        @ResourceId teamId: Long,
        postTeamMemberRequestDTO: PostTeamMemberRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        when (postTeamMemberRequestDTO.role) {
            TeamMemberRoleTypeDTO.OWNER -> {
                authorizationService.audit("ship-ownership", "team", teamId)
                teamService.shipTeamOwnershipToNoneMember(teamId, postTeamMemberRequestDTO.userId)
            }
            TeamMemberRoleTypeDTO.ADMIN -> {
                authorizationService.audit("add-admin", "team", teamId)
                teamService.addTeamAdmin(teamId, postTeamMemberRequestDTO.userId)
            }
            TeamMemberRoleTypeDTO.MEMBER -> {
                authorizationService.audit(
                    "add-normal-member",
                    "team",
                    teamId,
                    mapOf("member" to postTeamMemberRequestDTO.userId),
                )
                teamService.addTeamNormalMember(teamId, postTeamMemberRequestDTO.userId)
            }
        }
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(
            GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK")
        )
    }
}
