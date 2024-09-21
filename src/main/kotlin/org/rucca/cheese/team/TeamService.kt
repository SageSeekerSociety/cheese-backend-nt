package org.rucca.cheese.team

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.team.error.NotTeamMemberYetError
import org.rucca.cheese.team.error.TeamRoleConflictError
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
                id = team.id!!,
                name = team.name!!,
                intro = team.description!!,
                avatarId = team.avatar!!.id!!.toLong(),
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
        return relation.user!!.id!!.toLong()
    }

    fun isTeamAdmin(teamId: IdType, userId: IdType): Boolean {
        val relationOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        return relationOptional.isPresent &&
                (relationOptional.get().role == TeamMemberRole.ADMIN ||
                        relationOptional.get().role == TeamMemberRole.OWNER)
    }

    fun isTeamMember(teamId: IdType, userId: IdType): Boolean {
        val relationOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        return relationOptional.isPresent
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
                relations.map { userService.getUserDto(it.user!!.id!!.toLong()) }
    }

    fun getTeamMemberExamples(teamId: IdType): List<UserDTO> {
        val relations =
                teamUserRelationRepository.findAllByTeamIdAndRoleOrderByUpdatedAtDesc(
                        teamId,
                        TeamMemberRole.MEMBER,
                        PageRequest.of(0, 3),
                )
        return relations.map { userService.getUserDto(it.user!!.id!!.toLong()) }
    }

    fun ensureTeamNameNotExists(name: String) {
        if (teamRepository.existsByName(name)) {
            throw NameAlreadyExistsError("team", name)
        }
    }

    fun createTeam(name: String, description: String, avatarId: IdType, ownerId: IdType): IdType {
        ensureTeamNameNotExists(name)
        val team =
                teamRepository.save(
                        Team(name = name, description = description, avatar = Avatar().apply { id = avatarId.toInt() }))
        teamUserRelationRepository.save(
                TeamUserRelation(
                        team = team, role = TeamMemberRole.OWNER, user = User().apply { id = ownerId.toInt() }))
        return team.id!!
    }

    fun updateTeamName(teamId: IdType, name: String) {
        ensureTeamNameNotExists(name)
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        team.name = name
        teamRepository.save(team)
    }

    fun updateTeamDescription(teamId: IdType, description: String) {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        team.description = description
        teamRepository.save(team)
    }

    fun updateTeamAvatar(teamId: IdType, avatarId: IdType) {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        team.avatar = Avatar().apply { id = avatarId.toInt() }
        teamRepository.save(team)
    }

    fun deleteTeam(teamId: IdType) {
        val relations = teamUserRelationRepository.findAllByTeamId(teamId)
        relations.forEach { it.deletedAt = LocalDateTime.now() }
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        team.deletedAt = LocalDateTime.now()
        teamUserRelationRepository.saveAll(relations)
        teamRepository.save(team)
    }

    fun convertMemberRole(role: TeamMemberRole): TeamMemberRoleTypeDTO {
        return when (role) {
            TeamMemberRole.OWNER -> TeamMemberRoleTypeDTO.OWNER
            TeamMemberRole.ADMIN -> TeamMemberRoleTypeDTO.ADMIN
            TeamMemberRole.MEMBER -> TeamMemberRoleTypeDTO.MEMBER
        }
    }

    fun getTeamMemberRole(teamId: IdType, userId: IdType): TeamMemberRoleTypeDTO {
        val relation =
                teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId).orElseThrow {
                    NotTeamMemberYetError(teamId, userId)
                }
        return convertMemberRole(relation.role!!)
    }

    fun getTeamMembers(teamId: IdType): List<TeamMemberDTO> {
        val relations = teamUserRelationRepository.findAllByTeamId(teamId)
        return relations.map {
            TeamMemberDTO(
                    role = convertMemberRole(it.role!!),
                    user = userService.getUserDto(it.user!!.id!!.toLong()),
            )
        }
    }

    private fun addTeamMember(teamId: IdType, userId: IdType, role: TeamMemberRole) {
        val relationOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        if (relationOptional.isPresent) {
            throw TeamRoleConflictError(teamId, userId, relationOptional.get().role!!, role)
        } else {
            teamUserRelationRepository.save(
                    TeamUserRelation(
                            team = Team().apply { id = teamId },
                            role = role,
                            user = User().apply { id = userId.toInt() },
                    ))
        }
    }

    fun shipTeamOwnershipToNoneMember(teamId: IdType, userId: IdType) {
        val ownershipOriginal =
                teamUserRelationRepository.findByTeamIdAndRole(teamId, TeamMemberRole.OWNER).orElseThrow {
                    NotFoundError("team", teamId)
                }
        ownershipOriginal.role = TeamMemberRole.ADMIN
        val ownershipNewOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        if (ownershipNewOptional.isPresent) {
            throw TeamRoleConflictError(teamId, userId, ownershipNewOptional.get().role!!, TeamMemberRole.OWNER)
        } else {
            teamUserRelationRepository.save(ownershipOriginal)
            addTeamMember(teamId, userId, TeamMemberRole.OWNER)
        }
    }

    fun addTeamAdmin(teamId: IdType, userId: IdType) {
        addTeamMember(teamId, userId, TeamMemberRole.ADMIN)
    }

    fun addTeamNormalMember(teamId: IdType, userId: IdType) {
        addTeamMember(teamId, userId, TeamMemberRole.MEMBER)
    }

    fun removeTeamMember(teamId: IdType, userId: IdType) {
        val relation =
                teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId).orElseThrow {
                    NotTeamMemberYetError(teamId, userId)
                }
        relation.deletedAt = LocalDateTime.now()
        teamUserRelationRepository.save(relation)
    }
}
