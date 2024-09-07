package org.rucca.cheese.team

import org.rucca.cheese.api.TeamsApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TeamController : TeamsApi {
    override fun deleteTeam(teamId: Long): ResponseEntity<DeleteTeam200ResponseDTO> {
        return super.deleteTeam(teamId)
    }

    override fun deleteTeamMember(teamId: Long, userId: Long): ResponseEntity<GetTeam200ResponseDTO> {
        return super.deleteTeamMember(teamId, userId)
    }

    override fun getTeam(teamId: Long): ResponseEntity<GetTeam200ResponseDTO> {
        return super.getTeam(teamId)
    }

    override fun getTeamMembers(teamId: Long): ResponseEntity<GetTeamMembers200ResponseDTO> {
        return super.getTeamMembers(teamId)
    }

    override fun patchTeam(
            teamId: Long,
            patchTeamRequestDTO: PatchTeamRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.patchTeam(teamId, patchTeamRequestDTO)
    }

    override fun patchTeamMember(
            teamId: Long,
            userId: Long,
            patchTeamMemberRequestDTO: PatchTeamMemberRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.patchTeamMember(teamId, userId, patchTeamMemberRequestDTO)
    }

    override fun postTeam(postTeamRequestDTO: PostTeamRequestDTO): ResponseEntity<GetTeam200ResponseDTO> {
        return super.postTeam(postTeamRequestDTO)
    }

    override fun postTeamMember(
            teamId: Long,
            postTeamMemberRequestDTO: PostTeamMemberRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.postTeamMember(teamId, postTeamMemberRequestDTO)
    }
}
