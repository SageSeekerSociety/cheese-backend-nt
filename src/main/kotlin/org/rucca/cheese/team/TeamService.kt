package org.rucca.cheese.team

import java.time.LocalDateTime
import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.team.error.NotTeamMemberYetError
import org.rucca.cheese.team.error.TeamRoleConflictError
import org.rucca.cheese.user.Avatar
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Service

@Service
class TeamService(
        private val teamRepository: TeamRepository,
        private val teamUserRelationRepository: TeamUserRelationRepository,
        private val userService: UserService,
        private val elasticsearchTemplate: ElasticsearchTemplate,
) {
    fun getTeamDto(teamId: IdType): TeamDTO {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        return TeamDTO(
                id = team.id!!,
                name = team.name!!,
                intro = team.description!!,
                avatarId = team.avatar!!.id!!.toLong(),
                owner = userService.getUserDto(getTeamOwner(teamId)),
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
                updatedAt = team.updatedAt!!.toEpochMilli(),
                createdAt = team.createdAt!!.toEpochMilli(),
        )
    }

    fun getTeamAvatarId(teamId: IdType): IdType {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        return team.avatar!!.id!!.toLong()
    }

    fun getTaskParticipantSummaryDto(teamId: IdType): TaskParticipantSummaryDTO {
        val team = teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
        return TaskParticipantSummaryDTO(
                team.id!!,
                team.description!!,
                team.name!!,
                team.avatar!!.id!!.toLong(),
        )
    }

    fun getTeamOwner(teamId: IdType): IdType {
        val relation =
                teamUserRelationRepository.findByTeamIdAndRole(teamId, TeamMemberRole.OWNER).orElseThrow {
                    NotFoundError("team", teamId)
                }
        return relation.user!!.id!!.toLong()
    }

    fun enumerateTeams(query: String, pageStart: IdType?, pageSize: Int): Pair<List<TeamDTO>, PageDTO> {
        val id = query.toLongOrNull()
        if (id != null) {
            return Pair(listOf(getTeamDto(id)), PageDTO(id, 1, hasPrev = false, hasMore = false))
        }
        val criteria = Criteria("name").matches(query)
        val query = CriteriaQuery(criteria)
        val hints = elasticsearchTemplate.search(query, TeamElasticSearch::class.java)
        val result = (SearchHitSupport.unwrapSearchHits(hints) as List<*>).filterIsInstance<TeamElasticSearch>()
        val (teams, page) =
                PageHelper.pageFromAll(
                        result, pageStart, pageSize, { it.id!! }, { id -> throw NotFoundError("team", id) })
        return Pair(teams.map { getTeamDto(it.id!!) }, page)
    }

    fun getTeamsOfUser(userId: IdType): List<TeamDTO> {
        val relations = teamUserRelationRepository.findAllByUserId(userId)
        return relations.map { getTeamDto(it.team!!.id!!) }
    }

    fun getTeamsThatUserCanUseToJoinTask(taskId: IdType, userId: IdType): List<TeamSummaryDTO> {
        return teamUserRelationRepository.getTeamsThatUserCanUseToJoinTask(taskId, userId).map {
            TeamSummaryDTO(
                    id = it.id!!,
                    name = it.name!!,
                    intro = it.description!!,
                    avatarId = it.avatar!!.id!!.toLong(),
                    updatedAt = it.updatedAt!!.toEpochMilli(),
                    createdAt = it.createdAt!!.toEpochMilli(),
            )
        }
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
        return teamUserRelationRepository.countByTeamIdAndRole(teamId, TeamMemberRole.ADMIN)
    }

    fun countTeamNormalMembers(teamId: IdType): Int {
        return teamUserRelationRepository.countByTeamIdAndRole(teamId, TeamMemberRole.MEMBER)
    }

    fun getTeamAdminExamples(teamId: IdType): List<UserDTO> {
        val relations =
                teamUserRelationRepository.findAllByTeamIdAndRoleOrderByUpdatedAtDesc(
                        teamId,
                        TeamMemberRole.ADMIN,
                        PageRequest.of(0, 3),
                )
        return relations.map { userService.getUserDto(it.user!!.id!!.toLong()) }
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

    fun ensureTeamExists(teamId: IdType) {
        if (!teamRepository.existsById(teamId)) {
            throw NotFoundError("team", teamId)
        }
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
                    updatedAt = it.updatedAt!!.toEpochMilli(),
                    createdAt = it.createdAt!!.toEpochMilli(),
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
