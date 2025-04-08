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
import java.time.ZoneId
import java.util.Optional
import org.rucca.cheese.common.error.ConflictError
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.error.NameAlreadyExistsError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.team.error.NotTeamMemberYetError
import org.rucca.cheese.team.error.TeamRoleConflictError
import org.rucca.cheese.team.models.TeamMembershipApplication
import org.rucca.cheese.user.Avatar
import org.rucca.cheese.user.User
import org.rucca.cheese.user.services.UserRealNameService
import org.rucca.cheese.user.services.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

fun Team.toTeamSummaryDTO() =
    TeamSummaryDTO(
        id = this.id!!,
        name = this.name!!,
        intro = this.description!!,
        avatarId = this.avatar!!.id!!.toLong(),
        updatedAt = this.updatedAt.toEpochMilli(),
        createdAt = this.createdAt.toEpochMilli(),
    )

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val teamUserRelationRepository: TeamUserRelationRepository,
    private val userService: UserService,
    private val elasticsearchTemplate: ElasticsearchTemplate,
    private val userRealNameService: UserRealNameService,
) {
    /**
     * Retrieves the user IDs of members with ADMIN or OWNER roles for a given team.
     *
     * @param teamId The ID of the team.
     * @return A set of user IDs. Returns empty set if team not found or no admins/owners.
     */
    fun getTeamAdminAndOwnerIds(teamId: Long): Set<Long> {
        val relations =
            teamUserRelationRepository.findAllByTeamIdAndRoleIsIn(
                teamId,
                setOf(TeamMemberRole.ADMIN, TeamMemberRole.OWNER),
            )
        return relations.mapNotNull { it.user?.id?.toLong() }.toSet()
    }

    fun getTeamReference(teamId: IdType): Team {
        return teamRepository.getReferenceById(teamId)
    }

    fun getTeamSize(teamId: IdType): Int {
        return teamUserRelationRepository.countByTeamIdAndDeletedAtIsNull(teamId)
    }

    fun getTeamDto(teamId: IdType, currentUserId: IdType? = null): TeamDTO {
        val team = getTeam(teamId)
        val myRoleOptional =
            if (currentUserId != null)
                teamUserRelationRepository.findByTeamIdAndUserId(teamId, currentUserId)
            else Optional.empty()
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
            updatedAt = team.updatedAt.toEpochMilli(),
            createdAt = team.createdAt.toEpochMilli(),
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
        userId: IdType,
        query: String,
        pageStart: IdType?,
        pageSize: Int,
    ): Pair<List<TeamDTO>, PageDTO> {
        val id = query.toLongOrNull()
        if (id != null) {
            return Pair(listOf(getTeamDto(id, userId)), PageDTO(id, 1, hasMore = false))
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
                { id -> throw NotFoundError("team", id) },
            )
        return Pair(teams.map { getTeamDto(it.id!!, userId) }, page)
    }

    fun getTeamsOfUser(userId: IdType): List<TeamDTO> {
        val relations = teamUserRelationRepository.findAllByUserId(userId)
        return relations.map { getTeamDto(it.team!!.id!!, userId) }
    }

    fun getTeamSummariesOfUser(userId: IdType): List<TeamSummaryDTO> {
        val relations = teamUserRelationRepository.findAllByUserId(userId)
        return relations.map { it.team!!.toTeamSummaryDTO() }
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
        approveType: ApproveType,
    ): List<TeamSummaryDTO> {
        return teamRepository
            .getTeamsThatUserJoinedTaskAsWithApprovedType(taskId, userId, approveType)
            .map { it.toTeamSummaryDTO() }
    }

    fun isTeamAtLeastAdmin(teamId: IdType, userId: IdType): Boolean {
        val relationOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        return relationOptional.isPresent &&
            (relationOptional.get().role == TeamMemberRole.ADMIN ||
                relationOptional.get().role == TeamMemberRole.OWNER)
    }

    fun isTeamOwner(teamId: IdType, userId: IdType): Boolean {
        val relationOptional = teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId)
        return relationOptional.isPresent && relationOptional.get().role == TeamMemberRole.OWNER
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
        ownerId: IdType,
    ): IdType {
        ensureTeamNameNotExists(name)
        val team =
            teamRepository.save(
                Team(
                    name = name,
                    intro = intro,
                    description = description,
                    avatar = Avatar().apply { id = avatarId.toInt() },
                )
            )
        teamUserRelationRepository.save(
            TeamUserRelation(
                team = team,
                role = TeamMemberRole.OWNER,
                user = userService.getUserReference(ownerId),
            )
        )
        return team.id!!
    }

    private fun getTeam(teamId: IdType): Team {
        return teamRepository.findById(teamId).orElseThrow { NotFoundError("team", teamId) }
    }

    private fun getTeams(teamIds: List<IdType>): List<Team> {
        val teams = teamRepository.findAllById(teamIds)
        if (teams.size != teamIds.size) {
            val notFoundIds = teamIds.filter { id -> teams.none { it.id == id } }
            throw NotFoundError(
                "Team with IDs $notFoundIds not found",
                mapOf("teamIds" to notFoundIds),
            )
        }
        return teams
    }

    fun ensureTeamIdExists(teamId: IdType) {
        if (!teamRepository.existsById(teamId)) throw NotFoundError("team", teamId)
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

    fun getTeamMembers(
        teamId: IdType,
        queryRealNameStatus: Boolean = false,
    ): Pair<List<TeamMemberDTO>, Boolean?> {
        val relations = teamUserRelationRepository.findAllByTeamId(teamId)
        val members =
            relations.map {
                val user = userService.getUserDto(it.user!!.id!!.toLong())

                val hasRealNameInfo =
                    if (queryRealNameStatus) {
                        try {
                            userRealNameService.getUserIdentity(user.id)
                            true
                        } catch (e: Exception) {
                            false
                        }
                    } else null

                TeamMemberDTO(
                    role = convertMemberRole(it.role!!),
                    user = user,
                    updatedAt =
                        it.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    createdAt =
                        it.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    hasRealNameInfo = hasRealNameInfo,
                )
            }

        // 如果查询实名认证状态，计算是否所有成员都已认证
        val allMembersVerified =
            if (queryRealNameStatus) {
                members.all { it.hasRealNameInfo == true }
            } else null

        return Pair(members, allMembersVerified)
    }

    /**
     * Retrieves all teams where a user has admin privileges.
     *
     * @param userId The ID of the user.
     * @return A list of teams where the user is an admin.
     */
    fun getTeamsWhereUserIsAdmin(userId: IdType): List<Team> {
        return teamUserRelationRepository
            .findAllByUserId(userId)
            .filter { it.role == TeamMemberRole.OWNER || it.role == TeamMemberRole.ADMIN }
            .map { it.team!! }
    }

    /**
     * Retrieves the summary DTO of a team.
     *
     * @param teamId The ID of the team.
     * @return The team summary DTO.
     */
    fun getTeamSummaryDTO(teamId: IdType): TeamSummaryDTO {
        val team = getTeam(teamId)
        return team.toTeamSummaryDTO()
    }

    fun getTeamSummaryDTOs(teamIds: List<IdType>): Map<IdType, TeamSummaryDTO> {
        val teams = getTeams(teamIds)
        return teams.associate { it.id!! to it.toTeamSummaryDTO() }
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
                TeamMemberRole.OWNER,
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

    /**
     * Gets the real name verification status of all members in a team
     *
     * @param teamId The ID of the team
     * @return A pair containing a list of member status DTOs and whether all members are verified
     */
    fun getTeamMembersRealNameStatus(
        teamId: IdType
    ): Pair<List<TeamMemberRealNameStatusDTO>, Boolean> {
        // Check if team exists
        ensureTeamIdExists(teamId)

        // Get all team members with real name status
        val (members, allVerified) = getTeamMembers(teamId, true)

        // Map members to status DTOs
        val memberStatusList =
            members.map { member ->
                TeamMemberRealNameStatusDTO(
                    memberId = member.user.id,
                    hasRealNameInfo = member.hasRealNameInfo ?: false,
                    userName = member.user.username,
                )
            }

        return Pair(memberStatusList, allVerified ?: false)
    }

    /**
     * 获取用户是成员的所有团队
     *
     * @param userId 用户ID
     * @return 用户所在的所有团队列表
     */
    fun getTeamsWhereUserIsMember(userId: IdType): List<Team> {
        val teamUserRelations = teamUserRelationRepository.findAllByUserId(userId)
        return teamUserRelations.mapNotNull { it.team }
    }

    /**
     * Internal method to create a team membership relation. Should only be called after an
     * application/invitation is approved/accepted. Handles potential race conditions or conflicts.
     * Marked public for now, but consider package-private or internal visibility if possible.
     *
     * @param application The finalized (APPROVED or ACCEPTED) membership application.
     * @throws TeamRoleConflictError if the user somehow already has a role in the team.
     */
    @Transactional // Ensure atomicity
    fun createMembershipFromApplication(application: TeamMembershipApplication) {
        val teamId = application.team.id!!
        val userId = application.user.id!!
        val role = application.role // Role determined by the application

        // Check for existing relation *again* within the transaction for safety
        val existingRelation =
            teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId.toLong())
        if (existingRelation.isPresent) {
            // This case should ideally not happen due to prior checks, but handle defensively
            throw TeamRoleConflictError(
                teamId,
                userId.toLong(),
                existingRelation.get().role!!,
                role,
            )
        }

        // Create and save the new relation
        val newRelation =
            TeamUserRelation(
                team = application.team, // Use the reference from application
                user = application.user, // Use the reference from application
                role = role,
            )
        teamUserRelationRepository.save(newRelation)

        // Optionally: Invalidate caches, update team member counts, emit events etc.
    }

    /**
     * Removes a team member. Now likely called via a dedicated "leave team" or "kick member"
     * endpoint/service method. Requires checks for OWNER role (cannot remove owner directly, needs
     * ownership transfer).
     */
    @Transactional
    fun removeTeamMember(teamId: IdType, userIdToRemove: IdType, initiatorId: IdType) {
        // 1. Permission Check (e.g., initiator must be admin/owner OR the user themselves leaving)
        val initiatorRelation =
            teamUserRelationRepository.findByTeamIdAndUserId(teamId, initiatorId)
        val targetRelation =
            teamUserRelationRepository.findByTeamIdAndUserId(teamId, userIdToRemove).orElseThrow {
                NotTeamMemberYetError(teamId, userIdToRemove)
            } // Already checked if member?

        val isSelfLeave = initiatorId == userIdToRemove
        val isAdminOrOwner =
            initiatorRelation.isPresent &&
                (initiatorRelation.get().role == TeamMemberRole.ADMIN ||
                    initiatorRelation.get().role == TeamMemberRole.OWNER)

        if (!isSelfLeave && !isAdminOrOwner) {
            throw ForbiddenError(
                "User $initiatorId cannot remove member $userIdToRemove from team $teamId."
            )
        }

        // 2. Cannot remove the OWNER directly
        if (targetRelation.role == TeamMemberRole.OWNER) {
            throw ForbiddenError(
                "Cannot remove the team owner (ID: $userIdToRemove). Ownership must be transferred first."
            )
        }

        // 3. If admin tries to remove another admin/owner (policy decision needed)
        if (isAdminOrOwner && !isSelfLeave) {
            val initiatorRole = initiatorRelation.get().role!!
            val targetRole = targetRelation.role!!
            // Example policy: Admins cannot remove Owner. Admins cannot remove other Admins unless
            // they are Owner.
            if (targetRole == TeamMemberRole.OWNER)
                throw ForbiddenError("Admins cannot remove the Owner.")
            if (targetRole == TeamMemberRole.ADMIN && initiatorRole == TeamMemberRole.ADMIN) {
                throw ForbiddenError("Admins cannot remove other Admins.")
            }
        }

        // 4. Perform soft delete
        targetRelation.deletedAt = LocalDateTime.now() // Or OffsetDateTime? Be consistent.
        teamUserRelationRepository.save(targetRelation)

        // Optionally: Cancel any pending applications/invitations for this user/team?
        // Optionally: Update counts, emit events etc.
    }

    /**
     * Transfers team ownership. Requires careful validation. This logic remains largely the same
     * but should enforce that the target user *exists* and might need role adjustments.
     */
    @Transactional
    fun transferTeamOwnership(teamId: IdType, newOwnerId: IdType, initiatorId: IdType) {
        // 1. Permission Check: Only current owner can transfer
        val currentOwnerRelation =
            teamUserRelationRepository
                .findByTeamIdAndRole(teamId, TeamMemberRole.OWNER)
                .orElseThrow {
                    NotFoundError("Team Owner for team", teamId)
                } // Should always exist if team exists

        if (currentOwnerRelation.user?.id != initiatorId.toInt()) {
            throw ForbiddenError(
                "Only the current owner (ID: ${currentOwnerRelation.user?.id}) can transfer ownership."
            )
        }

        if (currentOwnerRelation.user?.id == newOwnerId.toInt()) {
            throw ConflictError(
                "Cannot transfer ownership to the current owner."
            ) // Or just return success?
        }

        // 2. Validate new owner exists
        val newOwnerUser =
            userService.getUserReference(newOwnerId) // Throws NotFoundError if user doesn't exist

        // 3. Handle new owner's existing relation (if any)
        val newOwnerExistingRelation =
            teamUserRelationRepository.findByTeamIdAndUserId(teamId, newOwnerId)

        if (newOwnerExistingRelation.isPresent) {
            val relation = newOwnerExistingRelation.get()
            if (relation.role == TeamMemberRole.OWNER) { // Should not happen if check above works
                throw ConflictError("User $newOwnerId is already the owner.")
            }
            // Update existing relation to OWNER
            relation.role = TeamMemberRole.OWNER
            teamUserRelationRepository.save(relation)
        } else {
            // Create new relation for the new owner
            val newRelation =
                TeamUserRelation(
                    team = getTeamReference(teamId), // Use reference
                    user = newOwnerUser,
                    role = TeamMemberRole.OWNER,
                )
            teamUserRelationRepository.save(newRelation)
        }

        // 4. Downgrade current owner to ADMIN (or remove? Policy decision - let's downgrade)
        currentOwnerRelation.role = TeamMemberRole.ADMIN
        teamUserRelationRepository.save(currentOwnerRelation)

        // Optionally: Invalidate caches, emit events etc.
    }

    fun findTeamUserRelation(teamId: IdType, userId: IdType): TeamUserRelation? {
        return teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId).orElse(null)
    }

    fun updateTeamMemberRole(teamId: IdType, userId: IdType, newRole: TeamMemberRole) {
        val relation =
            teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId).orElseThrow {
                NotTeamMemberYetError(teamId, userId)
            }
        relation.role = newRole
        teamUserRelationRepository.save(relation)
    }
}
