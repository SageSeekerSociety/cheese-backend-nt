package org.rucca.cheese.team

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TeamDTO
import org.springframework.stereotype.Service

@Service
class TeamService(
        private val teamRepository: TeamRepository,
        private val repository: TeamRepository,
        private val teamUserRelationRepository: TeamUserRelationRepository,
) {
    fun getTeamDto(teamId: IdType): TeamDTO {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        return TeamDTO(
                id = team.id,
                name = team.name,
                intro = team.description,
                avatarId = team.avatar.id!!.toLong(),
                admins = TODO(),
                members = TODO(),
        )
    }

    fun getTeamOwner(teamId: IdType): IdType {
        val relation =
                teamUserRelationRepository.findByTeamIdAndRole(teamId, TeamMemberRole.OWNER).orElseThrow {
                    NotFoundError("team", teamId)
                }
        return relation.user.id!!.toLong()
    }

    fun isTeamAdmin(teamId: IdType, userId: IdType): Boolean {
        val relationOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        return relationOptional.isPresent &&
                (relationOptional.get().role == TeamMemberRole.ADMIN ||
                        relationOptional.get().role == TeamMemberRole.OWNER)
    }
}
