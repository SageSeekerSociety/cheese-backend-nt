package org.rucca.cheese.task.service

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import java.time.ZoneId
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.*
import org.rucca.cheese.task.*
import org.rucca.cheese.task.error.NotTaskParticipantYetError
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskMembershipViewService(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskRepository: TaskRepository,
    private val entityManager: EntityManager,
    private val userService: UserService,
    private val teamService: TeamService,
    private val snapshotService: TaskMembershipSnapshotService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Helper to get Task
    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    /** Assembles the detailed TaskMembershipDTO for a single participant */
    @Transactional(readOnly = true)
    fun getTaskMembershipDTO(taskId: IdType, memberId: IdType): TaskMembershipDTO {
        val task = getTask(taskId)
        val membership =
            taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                NotTaskParticipantYetError(taskId, memberId)
            }

        val shouldDisplayRealNameInfo = task.requireRealName

        val individualRealNameInfo =
            if (task.submitterType == TaskSubmitterType.USER && shouldDisplayRealNameInfo) {
                // Delegate decryption/retrieval to SnapshotService
                snapshotService.getRealNameInfoFromMembership(membership)
            } else null

        val teamMemberSummaries =
            if (task.submitterType == TaskSubmitterType.TEAM) {
                // Delegate assembly of team member summaries
                getTeamParticipantMemberSummaries(task, membership, shouldDisplayRealNameInfo)
            } else null

        val taskParticipantSummaryDto =
            getParticipantSummary(task, membership, shouldDisplayRealNameInfo)

        return TaskMembershipDTO(
            id = membership.id!!,
            member = taskParticipantSummaryDto,
            createdAt =
                membership.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt =
                membership.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            deadline =
                membership.deadline?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            approved = membership.approved!!.convert(),
            realNameInfo = individualRealNameInfo,
            email = membership.email,
            phone = membership.phone,
            applyReason = membership.applyReason,
            personalAdvantage = membership.personalAdvantage,
            remark = membership.remark,
            teamMembers = teamMemberSummaries,
            completionStatus = membership.completionStatus.toDTO(),
        )
    }

    /** Assembles a list of TaskMembershipDTOs, optimized for bulk fetching */
    @Transactional(readOnly = true)
    fun getTaskMembershipDTOs(taskId: IdType, approveType: ApproveType?): List<TaskMembershipDTO> {
        val task = getTask(taskId)
        val shouldDisplayRealNameInfo = task.requireRealName

        // 1. Fetch memberships
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(TaskMembership::class.java)
        val root = cq.from(TaskMembership::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<Task>("task").get<IdType>("id"), taskId))
        approveType?.let { predicates.add(cb.equal(root.get<ApproveType>("approved"), it)) }
        cq.where(*predicates.toTypedArray())
        val participants = entityManager.createQuery(cq).resultList
        if (participants.isEmpty()) return emptyList()

        // 2. Bulk fetch User/Team DTOs for main participants
        val userMemberIds = participants.filter { !it.isTeam }.mapNotNull { it.memberId }
        val teamMemberIds = participants.filter { it.isTeam }.mapNotNull { it.memberId }
        val userDtoMap =
            if (userMemberIds.isNotEmpty()) userService.getUserDtos(userMemberIds) else emptyMap()
        val teamDtoMap =
            if (teamMemberIds.isNotEmpty()) teamService.getTeamDtos(teamMemberIds) else emptyMap()

        // 3. Bulk fetch User DTOs for ALL team members
        val allTeamMemberUserIds =
            participants
                .filter { it.isTeam }
                .flatMap { it.teamMembersRealNameInfo }
                .map { it.memberId }
                .distinct()
        val teamMemberUserDtoMap =
            if (allTeamMemberUserIds.isNotEmpty()) userService.getUserDtos(allTeamMemberUserIds)
            else emptyMap()

        // 4. Fetch team owner IDs
        val teamOwnerIdMap =
            teamMemberIds.associateWith { teamId ->
                try {
                    teamService.getTeamOwner(teamId)
                } catch (e: NotFoundError) {
                    null
                }
            }

        // 5. Map results
        return participants.map { membership ->
            val memberId = membership.memberId!!
            val dbMembershipId = membership.id!!
            val participantUuid = membership.participantUuid
            val isTeam = membership.isTeam

            // Build participant summary
            val participantSummary =
                if (isTeam) {
                    val teamDto =
                        teamDtoMap[memberId]
                            ?: throw IllegalStateException("TeamDTO not found for ID: $memberId")
                    if (shouldDisplayRealNameInfo) {
                        TaskParticipantSummaryDTO(
                            id = teamDto.id,
                            name = teamDto.name,
                            intro = "",
                            avatarId = 1,
                            participantId = participantUuid,
                        )
                    } else {
                        TaskParticipantSummaryDTO(
                            id = teamDto.id,
                            name = teamDto.name,
                            intro = teamDto.intro,
                            avatarId = teamDto.avatarId,
                            participantId = null,
                        )
                    }
                } else { // Is User
                    val userDto =
                        userDtoMap[memberId]
                            ?: throw IllegalStateException("UserDTO not found for ID: $memberId")
                    if (shouldDisplayRealNameInfo) {
                        TaskParticipantSummaryDTO(
                            id = 0,
                            name = "",
                            intro = "",
                            avatarId = 1,
                            participantId = participantUuid,
                        )
                    } else {
                        TaskParticipantSummaryDTO(
                            id = userDto.id,
                            name = userDto.username,
                            intro = userDto.intro,
                            avatarId = userDto.avatarId,
                            participantId = null,
                        )
                    }
                }

            // Build team member summaries
            val teamMemberSummaries: List<TaskTeamParticipantMemberSummaryDTO>? =
                if (isTeam) {
                    val teamOwnerId = teamOwnerIdMap[memberId]
                    membership.teamMembersRealNameInfo.map { memberSnapshot ->
                        val memberUserId = memberSnapshot.memberId
                        val participantMemberUuid = memberSnapshot.participantMemberUuid
                        val isLeader = memberUserId == teamOwnerId

                        if (shouldDisplayRealNameInfo) {
                            TaskTeamParticipantMemberSummaryDTO(
                                userId = null,
                                name = "",
                                intro = "",
                                avatarId = 1,
                                participantMemberId = participantMemberUuid,
                                // Delegate decryption to snapshot service
                                realNameInfo =
                                    snapshotService.getRealNameInfoForTeamMemberSnapshot(
                                        membership,
                                        memberSnapshot,
                                    ),
                                isLeader = isLeader,
                            )
                        } else {
                            val userDto = teamMemberUserDtoMap[memberUserId]!!
                            TaskTeamParticipantMemberSummaryDTO(
                                userId = memberUserId,
                                name = userDto.username,
                                intro = userDto.intro,
                                avatarId = userDto.avatarId,
                                participantMemberId = null,
                                realNameInfo = null,
                                isLeader = isLeader,
                            )
                        }
                    }
                } else null

            // Build individual real name info
            val individualRealNameInfo =
                if (!isTeam && shouldDisplayRealNameInfo) {
                    // Delegate decryption to snapshot service
                    snapshotService.getRealNameInfoFromMembership(membership)
                } else null

            // Construct final DTO
            TaskMembershipDTO(
                id = dbMembershipId,
                member = participantSummary,
                createdAt =
                    membership.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                updatedAt =
                    membership.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                deadline =
                    membership.deadline
                        ?.atZone(ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli(),
                approved = membership.approved!!.convert(),
                realNameInfo = individualRealNameInfo,
                email = membership.email,
                phone = membership.phone,
                applyReason = membership.applyReason,
                personalAdvantage = membership.personalAdvantage,
                remark = membership.remark,
                teamMembers = teamMemberSummaries,
                completionStatus = membership.completionStatus.toDTO(),
            )
        }
    }

    /** Gets summaries for each member within a team participation snapshot */
    private fun getTeamParticipantMemberSummaries(
        task: Task, // Needed for context (e.g., requireRealName)
        membership: TaskMembership,
        displayRealName: Boolean,
    ): List<TaskTeamParticipantMemberSummaryDTO> {
        if (membership.teamMembersRealNameInfo.isEmpty()) {
            logger.warn("Team membership ID {} has empty snapshot!", membership.id)
            return emptyList()
        }

        val teamOwnerId =
            teamService.getTeamOwner(
                membership.memberId!!
            ) // Could optimize if ownerId is on TeamDTO

        return membership.teamMembersRealNameInfo.map { memberSnapshot ->
            val memberUserId = memberSnapshot.memberId
            val isLeader = memberUserId == teamOwnerId

            if (displayRealName) {
                TaskTeamParticipantMemberSummaryDTO(
                    name = "",
                    intro = "",
                    avatarId = 1,
                    isLeader = isLeader,
                    participantMemberId = memberSnapshot.participantMemberUuid,
                    // Delegate decryption to snapshot service
                    realNameInfo =
                        snapshotService.getRealNameInfoForTeamMemberSnapshot(
                            membership,
                            memberSnapshot,
                        ),
                )
            } else {
                val user =
                    userService.getUserDto(
                        memberUserId
                    ) // Fetch user info if not displaying real name
                TaskTeamParticipantMemberSummaryDTO(
                    name = user.username,
                    intro = user.intro,
                    avatarId = user.avatarId,
                    userId = user.id,
                    isLeader = isLeader,
                    participantMemberId = memberSnapshot.participantMemberUuid, // Use UUID here too
                    realNameInfo = null,
                )
            }
        }
    }

    /** Gets the summary DTO for the main participant (User or Team) */
    private fun getParticipantSummary(
        task: Task,
        membership: TaskMembership,
        displayRealName: Boolean,
    ): TaskParticipantSummaryDTO {
        val memberId = membership.memberId!!
        return when (task.submitterType) {
            TaskSubmitterType.USER -> {
                if (displayRealName) {
                    TaskParticipantSummaryDTO(
                        id = 0,
                        intro = "",
                        name = "",
                        avatarId = 1,
                        participantId = membership.participantUuid,
                    )
                } else {
                    val user = userService.getUserDto(memberId)
                    TaskParticipantSummaryDTO(
                        id = user.id,
                        intro = user.intro,
                        name = user.username,
                        avatarId = user.avatarId,
                        participantId = membership.participantUuid,
                    ) // Use UUID
                }
            }
            TaskSubmitterType.TEAM -> {
                val team = teamService.getTeamDto(memberId)
                TaskParticipantSummaryDTO(
                    id = team.id,
                    intro = if (displayRealName) "" else team.intro,
                    name = team.name,
                    avatarId = team.avatarId,
                    participantId = membership.participantUuid, // Use UUID
                )
            }
        }
    }

    /** Gets info about whether user can submit */
    @Transactional(readOnly = true)
    fun getSubmittability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberIdAndApproved(
                        task.id!!,
                        userId,
                        ApproveType.APPROVED,
                    ),
                    null,
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserCanUseToSubmitTask(task.id!!, userId)
                Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    /** Gets info about whether user has joined */
    @Transactional(readOnly = true)
    fun getJoined(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                Pair(taskMembershipRepository.existsByTaskIdAndMemberId(task.id!!, userId), null)
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserJoinedTaskAs(task.id!!, userId)
                Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    /** Gets the user-specific deadline for a task */
    @Transactional(readOnly = true)
    fun getUserDeadline(taskId: IdType, userId: IdType): Long? {
        val task = getTask(taskId)
        val membership =
            when (task.submitterType) {
                TaskSubmitterType.USER -> {
                    taskMembershipRepository.findByTaskIdAndMemberId(taskId, userId).orElse(null)
                }
                TaskSubmitterType.TEAM -> {
                    teamService
                        .getTeamsThatUserJoinedTaskAsWithApprovedType(
                            taskId,
                            userId,
                            ApproveType.APPROVED,
                        )
                        .firstOrNull()
                        ?.let { team ->
                            taskMembershipRepository
                                .findByTaskIdAndMemberId(taskId, team.id)
                                .orElse(null)
                        }
                }
            }
        return membership
            ?.takeIf { it.approved == ApproveType.APPROVED }
            ?.deadline
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
    }

    /** Checks if the user is considered a participant in the task */
    @Transactional(readOnly = true)
    fun isTaskParticipant(taskId: IdType, userId: IdType): Boolean {
        val task = getTask(taskId)
        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                taskMembershipRepository.existsByTaskIdAndMemberId(taskId, userId)
            TaskSubmitterType.TEAM ->
                teamService.getTeamsThatUserJoinedTaskAs(taskId, userId).isNotEmpty()
        }
    }

    /** Provides info about how a user is participating */
    @Transactional(readOnly = true)
    fun getUserParticipationInfo(taskId: IdType, userId: IdType): TaskParticipationInfoDTO {
        val task = getTask(taskId)
        val identities = mutableListOf<TaskParticipationIdentityDTO>()

        when (task.submitterType) {
            TaskSubmitterType.USER -> {
                taskMembershipRepository.findByTaskIdAndMemberId(taskId, userId).ifPresent {
                    membership ->
                    identities.add(
                        TaskParticipationIdentityDTO(
                            id = membership.id!!,
                            type = TaskSubmitterTypeDTO.USER,
                            memberId = userId,
                            canSubmit = membership.approved == ApproveType.APPROVED,
                            approved = membership.approved!!.convert(),
                        )
                    )
                }
            }
            TaskSubmitterType.TEAM -> {
                val userTeams = teamService.getTeamsOfUser(userId)
                for (team in userTeams) {
                    val teamId = team.id
                    taskMembershipRepository.findByTaskIdAndMemberId(task.id!!, teamId).ifPresent {
                        membership ->
                        val canSubmitForThisTeam =
                            teamService.isTeamAtLeastAdmin(teamId, userId) &&
                                membership.approved == ApproveType.APPROVED
                        identities.add(
                            TaskParticipationIdentityDTO(
                                id = membership.id!!,
                                type = TaskSubmitterTypeDTO.TEAM,
                                memberId = teamId,
                                teamName = team.name,
                                canSubmit = canSubmitForThisTeam,
                                approved = membership.approved!!.convert(),
                            )
                        )
                    }
                }
            }
        }
        return TaskParticipationInfoDTO(
            identities = identities,
            hasParticipation = identities.isNotEmpty(),
        )
    }
}
