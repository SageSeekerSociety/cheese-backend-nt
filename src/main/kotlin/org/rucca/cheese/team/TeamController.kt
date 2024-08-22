package org.rucca.cheese.team

import org.rucca.cheese.api.TeamApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TeamController : TeamApi {
    override fun deleteTeam(team: Long): ResponseEntity<DeleteTeam200ResponseDTO> {
        return super.deleteTeam(team)
    }

    override fun deleteTeamMember(team: Long, user: Long): ResponseEntity<GetTeam200ResponseDTO> {
        return super.deleteTeamMember(team, user)
    }

    override fun patchTeam(
            team: Long,
            patchTeamRequestDTO: PatchTeamRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.patchTeam(team, patchTeamRequestDTO)
    }

    override fun getTeam(team: Long): ResponseEntity<GetTeam200ResponseDTO> {
        return super.getTeam(team)
    }

    override fun patchTeamMember(
            team: Long,
            user: Long,
            patchTeamMemberRequestDTO: PatchTeamMemberRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.patchTeamMember(team, user, patchTeamMemberRequestDTO)
    }

    override fun postTeam(postTeamRequestDTO: PostTeamRequestDTO): ResponseEntity<GetTeam200ResponseDTO> {
        return super.postTeam(postTeamRequestDTO)
    }

    override fun postTeamMember(
            team: Long,
            postTeamMemberRequestDTO: PostTeamMemberRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return super.postTeamMember(team, postTeamMemberRequestDTO)
    }
}
