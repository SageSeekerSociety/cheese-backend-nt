/*
 *  Description: This file implements the TaskTopicsService class.
 *               It is responsible for CRUD of a team.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.team

import java.time.LocalDateTime
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.ApproveType
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

fun Team.toTeamSummaryDTO() =
    TeamSummaryDTO(
        id = this.id!!,
        name = this.name!!,
        intro = this.description!!,
        avatarId = this.avatar!!.id!!.toLong(),
        updatedAt = this.updatedAt!!.toEpochMilli(),
        createdAt = this.createdAt!!.toEpochMilli(),
    )

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val teamUserRelationRepository: TeamUserRelationRepository,
    private val userService: UserService,
    private val elasticsearchTemplate: ElasticsearchTemplate,
    private val authenticateService: AuthenticationService,
) {
    fun convertApproveType(type: ApproveType): ApproveTypeDTO {
        return when (type) {
            ApproveType.APPROVED -> ApproveTypeDTO.APPROVED
            ApproveType.DISAPPROVED -> ApproveTypeDTO.DISAPPROVED
            ApproveType.NONE -> ApproveTypeDTO.NONE
        }
    }

    fun getTeamDto(teamId: IdType): TeamDTO {
        val team = getTeam(teamId)
        val currentUserId = authenticateService.getCurrentUserId()
        val myRoleOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, currentUserId)
        return TeamDTO(
            id = team.id!!,
            name = team.name!!,
            intro = team.intro!!,
            description = team.description!!,
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
            joined = myRoleOptional.isPresent,
            role = myRoleOptional.map { convertMemberRole(it.role!!) }.orElse(null),
        )
    }

    fun getTeamAvatarId(teamId: IdType): IdType {
        val team = getTeam(teamId)
        return team.avatar!!.id!!.toLong()
    }

    fun getTeamOwner(teamId: IdType): IdType {
        val relation =
            teamUserRelationRepository
                .findByTeamIdAndRole(teamId, TeamMemberRole.OWNER)
                .orElseThrow { NotFoundError("team", teamId) }
        return relation.user!!.id!!.toLong()
    }

    fun enumerateTeams(
        query: String,
        pageStart: IdType?,
        pageSize: Int
    ): Pair<List<TeamDTO>, PageDTO> {
        val id = query.toLongOrNull()
        if (id != null) {
            return Pair(listOf(getTeamDto(id)), PageDTO(id, 1, hasPrev = false, hasMore = false))
        }
        val criteria = Criteria("name").matches(query)
        val hints =
            elasticsearchTemplate.search(CriteriaQuery(criteria), TeamElasticSearch::class.java)
        val result =
            (SearchHitSupport.unwrapSearchHits(hints) as List<*>).filterIsInstance<
                TeamElasticSearch
            >()
        val (teams, page) =
            PageHelper.pageFromAll(
                result,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("team", id) }
            )
        return Pair(teams.map { getTeamDto(it.id!!) }, page)
    }

    fun getTeamsOfUser(userId: IdType): List<TeamDTO> {
        val relations = teamUserRelationRepository.findAllByUserId(userId)
        return relations.map { getTeamDto(it.team!!.id!!) }
    }

    fun getTeamsThatUserCanUseToJoinTask(taskId: IdType, userId: IdType): List<TeamSummaryDTO> {
        return teamRepository.getTeamsThatUserCanUseToJoinTask(taskId, userId).map {
            it.toTeamSummaryDTO()
        }
    }

    fun getTeamsThatUserCanUseToSubmitTask(taskId: IdType, userId: IdType): List<TeamSummaryDTO> {
        return teamRepository
            .getTeamsThatUserJoinedTaskAsWithApprovedType(taskId, userId, ApproveType.APPROVED)
            .map { it.toTeamSummaryDTO() }
    }

    fun getTeamsThatUserJoinedTaskAs(taskId: IdType, userId: IdType): List<TeamSummaryDTO> {
        return teamRepository.getTeamsThatUserJoinedTaskAs(taskId, userId).map {
            it.toTeamSummaryDTO()
        }
    }

    fun getTeamsThatUserJoinedTaskAsWithApprovedType(
        taskId: IdType,
        userId: IdType,
        approveType: ApproveType
    ): List<TeamSummaryDTO> {
        return teamRepository
            .getTeamsThatUserJoinedTaskAsWithApprovedType(taskId, userId, approveType)
            .map { it.toTeamSummaryDTO() }
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

    fun createTeam(
        name: String,
        intro: String,
        description: String,
        avatarId: IdType,
        ownerId: IdType
    ): IdType {
        ensureTeamNameNotExists(name)
        val team =
            teamRepository.save(
                Team(
                    name = name,
                    intro = intro,
                    description = description,
                    avatar = Avatar().apply { id = avatarId.toInt() }
                )
            )
        teamUserRelationRepository.save(
            TeamUserRelation(
                team = team,
                role = TeamMemberRole.OWNER,
                user = User().apply { id = ownerId.toInt() }
            )
        )
        return team.id!!
    }

    fun getTeam(teamId: IdType): Team {
        return teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
    }

    fun updateTeamName(teamId: IdType, name: String) {
        ensureTeamNameNotExists(name)
        val team = getTeam(teamId)
        team.name = name
        teamRepository.save(team)
    }

    fun updateTeamIntro(teamId: IdType, intro: String) {
        val team = getTeam(teamId)
        team.intro = intro
        teamRepository.save(team)
    }

    fun updateTeamDescription(teamId: IdType, description: String) {
        val team = getTeam(teamId)
        team.description = description
        teamRepository.save(team)
    }

    fun updateTeamAvatar(teamId: IdType, avatarId: IdType) {
        val team = getTeam(teamId)
        team.avatar = Avatar().apply { id = avatarId.toInt() }
        teamRepository.save(team)
    }

    fun existsTeam(teamId: IdType): Boolean {
        return teamRepository.existsById(teamId)
    }

    fun deleteTeam(teamId: IdType) {
        val relations = teamUserRelationRepository.findAllByTeamId(teamId)
        relations.forEach { it.deletedAt = LocalDateTime.now() }
        val team = getTeam(teamId)
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
                )
            )
        }
    }

    fun shipTeamOwnershipToNoneMember(teamId: IdType, userId: IdType) {
        val ownershipOriginal =
            teamUserRelationRepository
                .findByTeamIdAndRole(teamId, TeamMemberRole.OWNER)
                .orElseThrow { NotFoundError("team", teamId) }
        ownershipOriginal.role = TeamMemberRole.ADMIN
        val ownershipNewOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        if (ownershipNewOptional.isPresent) {
            throw TeamRoleConflictError(
                teamId,
                userId,
                ownershipNewOptional.get().role!!,
                TeamMemberRole.OWNER
            )
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
