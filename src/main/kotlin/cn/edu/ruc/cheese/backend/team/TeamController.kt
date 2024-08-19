package cn.edu.ruc.cheese.backend.team

import cn.edu.ruc.cheese.backend.api.TeamApi
import cn.edu.ruc.cheese.backend.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TeamController : TeamApi {
    override fun deleteTeam(team: Int): ResponseEntity<DeleteTeam200Response> {
        return super.deleteTeam(team)
    }

    override fun deleteTeamMember(team: Int, user: Int): ResponseEntity<GetTeam200Response> {
        return super.deleteTeamMember(team, user)
    }

    override fun getTeam(team: Int): ResponseEntity<GetTeam200Response> {
        return super.getTeam(team)
    }

    override fun patchTeam(team: Int, patchTeamRequest: PatchTeamRequest?): ResponseEntity<GetTeam200Response> {
        return super.patchTeam(team, patchTeamRequest)
    }

    override fun patchTeamMember(
            team: Int,
            user: Int,
            patchTeamMemberRequest: PatchTeamMemberRequest?
    ): ResponseEntity<GetTeam200Response> {
        return super.patchTeamMember(team, user, patchTeamMemberRequest)
    }

    override fun postTeam(postTeamRequest: PostTeamRequest?): ResponseEntity<GetTeam200Response> {
        return super.postTeam(postTeamRequest)
    }

    override fun postTeamMember(
            team: Int,
            postTeamMemberRequest: PostTeamMemberRequest?
    ): ResponseEntity<GetTeam200Response> {
        return super.postTeamMember(team, postTeamMemberRequest)
    }
}
