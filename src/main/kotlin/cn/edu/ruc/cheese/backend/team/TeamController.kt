package cn.edu.ruc.cheese.backend.team

import cn.edu.ruc.cheese.backend.api.TeamApi
import cn.edu.ruc.cheese.backend.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TeamController : TeamApi {
    override fun deleteTeam(team: Int?): ResponseEntity<InlineResponse2005> {
        return super.deleteTeam(team)
    }

    override fun deleteTeamMember(team: Int?, user: Int?): ResponseEntity<InlineResponse2004> {
        return super.deleteTeamMember(team, user)
    }

    override fun getTeam(team: Int?): ResponseEntity<InlineResponse2004> {
        return super.getTeam(team)
    }

    override fun patchTeam(team: Int?, body: TeamBody1?): ResponseEntity<InlineResponse2004> {
        return super.patchTeam(team, body)
    }

    override fun patchTeamMember(team: Int?, user: Int?, body: TeamMemberBody1?): ResponseEntity<InlineResponse2004> {
        return super.patchTeamMember(team, user, body)
    }

    override fun postTeam(body: TeamBody?): ResponseEntity<InlineResponse2004> {
        return super.postTeam(body)
    }

    override fun postTeamMember(team: Int?, body: TeamMemberBody?): ResponseEntity<InlineResponse2004> {
        return super.postTeamMember(team, body)
    }
}
