package org.rucca.cheese.team

import javax.annotation.PostConstruct
import org.rucca.cheese.api.TeamsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.NoAuth
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TeamController(
        private val teamService: TeamService,
        private val authorizationService: AuthorizationService,
        private val authenticationService: AuthenticationService,
) : TeamsApi {
    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register("team", teamService::getTeamOwner)
        authorizationService.customAuthLogics.register("is-team-admin") {
                userId: IdType,
                _: AuthorizedAction,
                _: String,
                resourceId: IdType?,
                _: IdGetter?,
                _: Any?,
            ->
            teamService.isTeamAdmin(resourceId ?: throw IllegalArgumentException("resourceId is null"), userId)
        }
    }

    @Guard("delete", "team")
    override fun deleteTeam(@ResourceId teamId: Long): ResponseEntity<DeleteTeam200ResponseDTO> {
        return super.deleteTeam(teamId)
    }

    @NoAuth
    override fun deleteTeamMember(@ResourceId teamId: Long, userId: Long): ResponseEntity<GetTeam200ResponseDTO> {
        // TODO: Validate "remove-admin" or "remove-member" permission
        return super.deleteTeamMember(teamId, userId)
    }

    @Guard("query", "team")
    override fun getTeam(@ResourceId teamId: Long): ResponseEntity<GetTeam200ResponseDTO> {
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK"))
    }

    @Guard("enumerate-members", "team")
    override fun getTeamMembers(@ResourceId teamId: Long): ResponseEntity<GetTeamMembers200ResponseDTO> {
        return super.getTeamMembers(teamId)
    }

    @Guard("modify", "team")
    override fun patchTeam(
            @ResourceId teamId: Long,
            patchTeamRequestDTO: PatchTeamRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.patchTeam(teamId, patchTeamRequestDTO)
    }

    @Guard("modify-member", "team")
    override fun patchTeamMember(
            @ResourceId teamId: Long,
            userId: Long,
            patchTeamMemberRequestDTO: PatchTeamMemberRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        // TODO: Validate "ship-ownership" permission
        return super.patchTeamMember(teamId, userId, patchTeamMemberRequestDTO)
    }

    @Guard("create", "team")
    override fun postTeam(postTeamRequestDTO: PostTeamRequestDTO): ResponseEntity<GetTeam200ResponseDTO> {
        val teamId =
                teamService.createTeam(
                        postTeamRequestDTO.name,
                        postTeamRequestDTO.intro,
                        postTeamRequestDTO.avatarId,
                        authenticationService.getCurrentUserId())
        val teamDTO = teamService.getTeamDto(teamId)
        return ResponseEntity.ok(GetTeam200ResponseDTO(200, GetTeam200ResponseDataDTO(teamDTO), "OK"))
    }

    @NoAuth
    override fun postTeamMember(
            @ResourceId teamId: Long,
            postTeamMemberRequestDTO: PostTeamMemberRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        // TODO: Validate "ship-ownership", "add-admin" or "add-normal-member" permission
        return super.postTeamMember(teamId, postTeamMemberRequestDTO)
    }
}
