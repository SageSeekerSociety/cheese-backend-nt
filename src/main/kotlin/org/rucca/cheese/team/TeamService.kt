package org.rucca.cheese.team

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TeamAdminsDTO
import org.rucca.cheese.model.TeamDTO
import org.rucca.cheese.model.TeamMembersDTO
import org.rucca.cheese.model.UserDTO
import org.rucca.cheese.user.Avatar
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class TeamService(
        private val teamRepository: TeamRepository,
        private val teamUserRelationRepository: TeamUserRelationRepository,
        private val userService: UserService,
) {
    fun getTeamDto(teamId: IdType): TeamDTO {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        return TeamDTO(
                id = team.id,
                name = team.name,
                intro = team.description,
                avatarId = team.avatar.id!!.toLong(),
                admins =
                        TeamAdminsDTO(
                                total = countTeamAdmins(teamId),
                                examples = getTeamAdminExamples(teamId),
                        ),
                members =
                        TeamMembersDTO(
                                total = countTeamNormalMembers(teamId),
                                examples = getTeamMemberExamples(teamId),
                        ),
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

    fun countTeamAdmins(teamId: IdType): Int {
        return teamUserRelationRepository.countByTeamIdAndRole(teamId, TeamMemberRole.ADMIN) + 1
    }

    fun countTeamNormalMembers(teamId: IdType): Int {
        return teamUserRelationRepository.countByTeamIdAndRole(teamId, TeamMemberRole.MEMBER)
    }

    fun getTeamAdminExamples(teamId: IdType): List<UserDTO> {
        val relations =
                teamUserRelationRepository.findAllByTeamIdAndRoleOrderByUpdatedAtDesc(
                        teamId,
                        TeamMemberRole.ADMIN,
                        PageRequest.of(0, 2),
                )
        return listOf(userService.getUserDto(getTeamOwner(teamId))) +
                relations.map { userService.getUserDto(it.user.id!!.toLong()) }
    }

    fun getTeamMemberExamples(teamId: IdType): List<UserDTO> {
        val relations =
                teamUserRelationRepository.findAllByTeamIdAndRoleOrderByUpdatedAtDesc(
                        teamId,
                        TeamMemberRole.MEMBER,
                        PageRequest.of(0, 3),
                )
        return relations.map { userService.getUserDto(it.user.id!!.toLong()) }
    }

    fun createTeam(name: String, description: String, avatarId: IdType, ownerId: IdType): IdType {
        val team =
                teamRepository.save(
                        Team(name = name, description = description, avatar = Avatar().apply { id = avatarId.toInt() }))
        teamUserRelationRepository.save(
                TeamUserRelation(
                        team = team, role = TeamMemberRole.OWNER, user = User().apply { id = ownerId.toInt() }))
        return team.id
    }
}
