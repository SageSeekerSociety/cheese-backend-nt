/*
 *  Description: This file implements the TaskMembershipService class.
 *               It is responsible for CRUD of task's membership.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.task

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import java.time.LocalDateTime
import java.time.ZoneId
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.*
import org.rucca.cheese.space.SpaceUserRankService
import org.rucca.cheese.task.error.*
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.models.KeyPurpose
import org.rucca.cheese.user.services.EncryptionService
import org.rucca.cheese.user.services.UserRealNameService
import org.rucca.cheese.user.services.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

val DefaultRealNameInfo =
    RealNameInfo(
        realName = "",
        studentId = "",
        grade = "",
        major = "",
        className = "",
        encrypted = false,
    )

fun RealNameInfo.convert(): TaskParticipantRealNameInfoDTO {
    return TaskParticipantRealNameInfoDTO(
        realName = realName ?: "",
        studentId = studentId ?: "",
        grade = grade ?: "",
        major = major ?: "",
        className = className ?: "",
    )
}

fun TaskParticipantRealNameInfoDTO.convert(): RealNameInfo {
    return RealNameInfo(
        realName = realName,
        studentId = studentId,
        grade = grade,
        major = major,
        className = className,
        encrypted = false,
    )
}

@Service
class TaskMembershipService(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskRepository: TaskRepository,
    private val userService: UserService,
    private val teamService: TeamService,
    private val applicationConfig: ApplicationConfig,
    private val spaceUserRankService: SpaceUserRankService,
    private val entityManager: EntityManager,
    private val userRealNameService: UserRealNameService,
    private val encryptionService: EncryptionService,
    private val entityPatcher: EntityPatcher,
) {
    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    private fun getTaskMembership(taskId: IdType, memberId: IdType): TaskMembership {
        return taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
            NotTaskParticipantYetError(taskId, memberId)
        }
    }

    private fun getTaskMembership(participantId: IdType): TaskMembership {
        return taskMembershipRepository.findById(participantId).orElseThrow {
            NotFoundError("task participant", participantId)
        }
    }

    fun getTaskParticipantMemberId(participantId: IdType): IdType {
        return taskMembershipRepository
            .findById(participantId)
            .orElseThrow { NotFoundError("task participant", participantId) }
            .memberId!!
    }

    fun getTaskMembershipDTO(
        taskId: IdType,
        memberId: IdType,
        queryRealNameInfo: Boolean = false,
    ): TaskMembershipDTO {
        val task = getTask(taskId)
        val membership = getTaskMembership(taskId, memberId)

        val shouldQueryRealNameInfo = queryRealNameInfo && task.requireRealName

        val realNameInfo =
            if (shouldQueryRealNameInfo) {
                getRealNameInfo(task, membership)
            } else null

        val taskParticipantSummaryDto =
            getParticipantSummary(task, membership, memberId, shouldQueryRealNameInfo)

        return TaskMembershipDTO(
            id = membership.id!!,
            member = taskParticipantSummaryDto,
            createdAt =
                membership.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt =
                membership.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            deadline =
                membership.deadline?.let {
                    it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                },
            approved = membership.approved!!.convert(),
            realNameInfo = realNameInfo,
            applyReason = membership.applyReason,
            personalAdvantage = membership.personalAdvantage,
            remark = membership.remark,
        )
    }

    private fun getRealNameInfo(
        task: Task,
        membership: TaskMembership,
    ): TaskParticipantRealNameInfoDTO? {
        return when {
            // For individual participants
            task.submitterType == TaskSubmitterType.USER && membership.realNameInfo != null -> {
                if (membership.realNameInfo!!.encrypted && membership.encryptionKeyId != null) {
                    decryptUserRealNameInfo(membership.realNameInfo!!, membership.encryptionKeyId!!)
                } else {
                    membership.realNameInfo!!.convert()
                }
            }

            // For team participants
            task.submitterType == TaskSubmitterType.TEAM &&
                membership.teamMembersRealNameInfo.isNotEmpty() -> {
                membership.teamMembersRealNameInfo.firstOrNull()?.let { firstTeamMember ->
                    membership.encryptionKeyId?.let { key ->
                        if (firstTeamMember.realNameInfo.encrypted) {
                            decryptTeamMemberRealNameInfo(firstTeamMember.realNameInfo, key)
                        } else {
                            firstTeamMember.realNameInfo.convert()
                        }
                    }
                }
            }

            else -> null
        }
    }

    private fun decryptUserRealNameInfo(
        encryptedInfo: RealNameInfo,
        keyId: String,
    ): TaskParticipantRealNameInfoDTO {
        return RealNameInfo(
                realName = encryptionService.decryptData(encryptedInfo.realName!!, keyId),
                studentId = encryptionService.decryptData(encryptedInfo.studentId!!, keyId),
                grade = encryptionService.decryptData(encryptedInfo.grade!!, keyId),
                major = encryptionService.decryptData(encryptedInfo.major!!, keyId),
                className = encryptionService.decryptData(encryptedInfo.className!!, keyId),
                encrypted = false,
            )
            .convert()
    }

    private fun decryptTeamMemberRealNameInfo(
        encryptedInfo: RealNameInfo,
        keyId: String,
    ): TaskParticipantRealNameInfoDTO {
        return RealNameInfo(
                realName = encryptionService.decryptData(encryptedInfo.realName!!, keyId),
                studentId = encryptionService.decryptData(encryptedInfo.studentId!!, keyId),
                grade = encryptionService.decryptData(encryptedInfo.grade!!, keyId),
                major = encryptionService.decryptData(encryptedInfo.major!!, keyId),
                className = encryptionService.decryptData(encryptedInfo.className!!, keyId),
                encrypted = false,
            )
            .convert()
    }

    private fun getParticipantSummary(
        task: Task,
        membership: TaskMembership,
        memberId: IdType,
        isRealNameInfoQuery: Boolean,
    ): TaskParticipantSummaryDTO {
        return if (!isRealNameInfoQuery) {
            // Regular participant summary
            when (task.submitterType!!) {
                TaskSubmitterType.USER -> {
                    val user = userService.getUserDto(memberId)
                    TaskParticipantSummaryDTO(
                        id = user.id,
                        intro = user.intro,
                        name = user.username,
                        avatarId = user.avatarId,
                    )
                }
                TaskSubmitterType.TEAM -> {
                    val team = teamService.getTeamDto(memberId)
                    TaskParticipantSummaryDTO(
                        id = team.id,
                        intro = team.intro,
                        name = team.name,
                        avatarId = team.avatarId,
                    )
                }
            }
        } else {
            // Minimal info when using real name info
            when (task.submitterType!!) {
                TaskSubmitterType.USER -> {
                    TaskParticipantSummaryDTO(
                        id = membership.id!!,
                        intro = "",
                        name = "", // Empty name when using real name info
                        avatarId = 1, // Default avatar ID
                    )
                }

                TaskSubmitterType.TEAM -> {
                    // For teams, show team name even with real name info
                    val team = teamService.getTeamDto(memberId)
                    TaskParticipantSummaryDTO(
                        id = team.id,
                        intro = "",
                        name = team.name,
                        avatarId = 1, // Default avatar ID
                    )
                }
            }
        }
    }

    fun getTaskMembershipDTOs(
        taskId: IdType,
        approveType: ApproveType?,
        queryRealNameInfo: Boolean = false,
    ): List<TaskMembershipDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(TaskMembership::class.java)
        val root = cq.from(TaskMembership::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<Task>("task").get<IdType>("id"), taskId))
        if (approveType != null) {
            predicates.add(cb.equal(root.get<ApproveType>("approved"), approveType))
        }
        cq.where(*predicates.toTypedArray())
        val query = entityManager.createQuery(cq)
        val participants = query.resultList
        return participants.map { getTaskMembershipDTO(taskId, it.memberId!!, queryRealNameInfo) }
    }

    fun addTaskParticipant(
        taskId: IdType,
        memberId: IdType,
        deadline: LocalDateTime?,
        approved: ApproveType,
        email: String?,
        phone: String?,
        applyReason: String?,
        personalAdvantage: String?,
        remark: String?,
    ): TaskMembershipDTO {
        if (email.isNullOrBlank() && phone.isNullOrBlank()) {
            throw EmailOrPhoneRequiredError(taskId, memberId)
        }
        val task = getTask(taskId)
        val errorOpt = isTaskJoinable(task, memberId)
        if (errorOpt != null) throw errorOpt

        var memberRealNameInfo: RealNameInfo? = null
        val isTeam = task.submitterType == TaskSubmitterType.TEAM
        val teamMembersRealNameInfo = mutableListOf<TeamMemberRealNameInfo>()

        // Process real name information
        if (task.requireRealName == true) {
            when (task.submitterType!!) {
                TaskSubmitterType.USER -> {
                    // For individual users, copy from their user real name info
                    try {
                        val userIdentity = userRealNameService.getUserIdentity(memberId)

                        // Get or create encryption key for this task membership
                        val key =
                            encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)

                        // Create encrypted real name info
                        memberRealNameInfo =
                            RealNameInfo(
                                realName =
                                    encryptionService.encryptData(userIdentity.realName, key.id),
                                studentId =
                                    encryptionService.encryptData(userIdentity.studentId, key.id),
                                grade = encryptionService.encryptData(userIdentity.grade, key.id),
                                major = encryptionService.encryptData(userIdentity.major, key.id),
                                className =
                                    encryptionService.encryptData(userIdentity.className, key.id),
                                encrypted = true,
                            )
                    } catch (e: NotFoundError) {
                        throw RealNameInfoRequiredError(memberId)
                    }
                }

                TaskSubmitterType.TEAM -> {
                    // For teams, collect real name info from all team members
                    memberRealNameInfo = DefaultRealNameInfo

                    // Get the encryption key for this task
                    val key = encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)

                    // Get all team members
                    val (teamMembers, _) = teamService.getTeamMembers(memberId)

                    // For each team member, get their real name info and encrypt it
                    for (teamMember in teamMembers) {
                        try {
                            val userIdentity =
                                userRealNameService.getUserIdentity(teamMember.user.id)

                            // Create encrypted real name info for this team member
                            val memberInfo =
                                RealNameInfo(
                                    realName =
                                        encryptionService.encryptData(
                                            userIdentity.realName,
                                            key.id,
                                        ),
                                    studentId =
                                        encryptionService.encryptData(
                                            userIdentity.studentId,
                                            key.id,
                                        ),
                                    grade =
                                        encryptionService.encryptData(userIdentity.grade, key.id),
                                    major =
                                        encryptionService.encryptData(userIdentity.major, key.id),
                                    className =
                                        encryptionService.encryptData(
                                            userIdentity.className,
                                            key.id,
                                        ),
                                    encrypted = true,
                                )

                            // Add to the list of team members' real name info
                            teamMembersRealNameInfo.add(
                                TeamMemberRealNameInfo(
                                    memberId = teamMember.user.id,
                                    realNameInfo = memberInfo,
                                )
                            )
                        } catch (e: NotFoundError) {
                            // If any team member doesn't have real name info, throw an error
                            throw RealNameInfoRequiredError(teamMember.user.id)
                        }
                    }
                }
            }
        }

        val membership =
            taskMembershipRepository.save(
                TaskMembership(
                    task = task,
                    memberId = memberId,
                    deadline = deadline,
                    approved = approved,
                    isTeam = isTeam,
                    realNameInfo = memberRealNameInfo,
                    teamMembersRealNameInfo = teamMembersRealNameInfo,
                    email = email ?: "",
                    phone = phone ?: "",
                    applyReason = applyReason ?: "",
                    personalAdvantage = personalAdvantage ?: "",
                    remark = remark ?: "",
                    encryptionKeyId =
                        if (
                            (memberRealNameInfo != null && memberRealNameInfo.encrypted) ||
                                teamMembersRealNameInfo.isNotEmpty()
                        )
                            encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId).id
                        else null,
                )
            )
        autoRejectParticipantAfterReachesLimit(taskId)
        return getTaskMembershipDTO(taskId, membership.memberId!!, queryRealNameInfo = true)
    }

    @Transactional
    fun updateTaskMembership(
        taskId: IdType,
        memberId: IdType,
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO,
    ): TaskMembershipDTO {
        val participant = getTaskMembership(taskId, memberId)

        if (patchTaskMembershipRequestDTO.approved == ApproveTypeDTO.APPROVED) {
            ensureTaskParticipantNotReachedLimit(taskId)
        }

        if (
            patchTaskMembershipRequestDTO.deadline != null &&
                participant.approved != ApproveType.APPROVED &&
                patchTaskMembershipRequestDTO.approved != ApproveTypeDTO.APPROVED
        ) {
            throw ForbiddenError(
                "Cannot set deadline for non-approved task membership",
                mapOf("taskId" to participant.task!!.id!!, "participantId" to participant.id!!),
            )
        }

        entityPatcher.patch(participant, patchTaskMembershipRequestDTO) {
            handle(PatchTaskMembershipRequestDTO::deadline) { entity, value ->
                entity.deadline = value.toLocalDateTime()
            }
            handle(PatchTaskMembershipRequestDTO::approved) { entity, value ->
                entity.approved = value.convert()
            }
        }
        taskMembershipRepository.save(participant)

        if (patchTaskMembershipRequestDTO.approved == ApproveTypeDTO.APPROVED) {
            autoRejectParticipantAfterReachesLimit(taskId)
        }

        return getTaskMembershipDTO(taskId, memberId)
    }

    @Transactional
    fun updateTaskMembership(
        participantId: IdType,
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO,
    ): TaskMembershipDTO {
        val participant = getTaskMembership(participantId)

        if (patchTaskMembershipRequestDTO.approved == ApproveTypeDTO.APPROVED) {
            ensureTaskParticipantNotReachedLimit(participant.task!!.id!!)
        }

        if (
            patchTaskMembershipRequestDTO.deadline != null &&
                participant.approved != ApproveType.APPROVED &&
                patchTaskMembershipRequestDTO.approved != ApproveTypeDTO.APPROVED
        ) {
            throw ForbiddenError(
                "Cannot set deadline for non-approved task membership",
                mapOf("taskId" to participant.task!!.id!!, "participantId" to participantId),
            )
        }

        entityPatcher.patch(participant, patchTaskMembershipRequestDTO) {
            handle(PatchTaskMembershipRequestDTO::deadline) { entity, value ->
                entity.deadline = value.toLocalDateTime()
            }
            handle(PatchTaskMembershipRequestDTO::approved) { entity, value ->
                entity.approved = value.convert()
            }
        }
        taskMembershipRepository.save(participant)

        if (patchTaskMembershipRequestDTO.approved == ApproveTypeDTO.APPROVED) {
            autoRejectParticipantAfterReachesLimit(participant.task!!.id!!)
        }

        return getTaskMembershipDTO(participant.task!!.id!!, participant.memberId!!)
    }

    fun updateTaskMembershipDeadline(taskId: IdType, memberId: IdType, deadline: Long) {
        val participant = getTaskMembership(taskId, memberId)
        if (participant.approved == ApproveType.APPROVED) {
            participant.deadline = deadline.toLocalDateTime()
        } else {
            throw TaskParticipantNotApprovedError(taskId, memberId)
        }
        taskMembershipRepository.save(participant)
    }

    fun ensureTaskParticipantNotReachedLimit(taskId: IdType) {
        if (applicationConfig.enforceTaskParticipantLimitCheck) {
            val task = getTask(taskId)
            if (task.participantLimit != null) {
                val actual =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actual >= task.participantLimit!!)
                    throw TaskParticipantsReachedLimitError(taskId, task.participantLimit!!, actual)
            }
        }
    }

    fun autoRejectParticipantAfterReachesLimit(taskId: IdType) {
        if (applicationConfig.autoRejectParticipantAfterReachesLimit) {
            val task = getTask(taskId)
            if (task.participantLimit != null) {
                val actual =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actual >= task.participantLimit!!) {
                    val participants =
                        taskMembershipRepository.findAllByTaskIdAndApproved(
                            taskId,
                            ApproveType.NONE,
                        )
                    participants.forEach {
                        it.approved = ApproveType.DISAPPROVED
                        taskMembershipRepository.save(it)
                    }
                }
            }
        }
    }

    @Transactional
    fun removeTaskParticipant(taskId: IdType, participantId: IdType) {
        val participant =
            taskMembershipRepository.findById(participantId).orElseThrow {
                NotFoundError("task participant", participantId)
            }
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
    }

    @Transactional
    fun removeTaskParticipantByMemberId(taskId: IdType, memberId: IdType) {
        val participant =
            taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                NotTaskParticipantYetError(taskId, memberId)
            }
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
    }

    fun isTaskJoinable(task: Task, memberId: IdType): BaseError? {
        // Task is approved
        if (task.approved != ApproveType.APPROVED)
            return ForbiddenError(
                "Task ${task.id} is not approved yet",
                mapOf("taskId" to task.id!!),
            )

        // Ensure member exists
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                if (!userService.existsUser(memberId)) return NotFoundError("user", memberId)
            TaskSubmitterType.TEAM ->
                if (!teamService.existsTeam(memberId)) return NotFoundError("team", memberId)
        }

        // Has not joined yet
        if (taskMembershipRepository.existsByTaskIdAndMemberId(task.id!!, memberId))
            return AlreadyBeTaskParticipantError(task.id!!, memberId)

        // Check if user has real name info if required
        if (task.requireRealName == true) {
            when (task.submitterType) {
                TaskSubmitterType.USER -> {
                    // For individual participants, check if they have real name info
                    try {
                        userRealNameService.getUserIdentity(memberId)
                    } catch (e: NotFoundError) {
                        return RealNameInfoRequiredError(memberId)
                    }
                }

                TaskSubmitterType.TEAM -> {
                    // For team participants, check if all team members have real name info
                    val (teamMembers, _) = teamService.getTeamMembers(memberId)
                    for (teamMember in teamMembers) {
                        try {
                            userRealNameService.getUserIdentity(teamMember.user.id)
                        } catch (e: NotFoundError) {
                            return RealNameInfoRequiredError(teamMember.user.id)
                        }
                    }
                }
            }
        }

        // Have enough rank
        val needToCheckRank =
            applicationConfig.rankCheckEnforced &&
                task.space != null &&
                task.space.enableRank!! &&
                task.rank != null
        if (needToCheckRank) {
            val requiredRank = task.rank!! - applicationConfig.rankJump
            if (task.submitterType == TaskSubmitterType.USER) {
                val actualRank = spaceUserRankService.getRank(task.space!!.id!!, memberId)
                if (actualRank < requiredRank)
                    return YourRankIsNotHighEnoughError(actualRank, requiredRank)
            } else {
                // For team tasks, check if all team members have enough rank
                val (teamMembers, _) = teamService.getTeamMembers(memberId)
                for (teamMember in teamMembers) {
                    val actualRank =
                        spaceUserRankService.getRank(task.space!!.id!!, teamMember.user.id)
                    if (actualRank < requiredRank) {
                        return YourTeamMemberRankIsNotHighEnoughError(
                            teamMember.user.id,
                            actualRank,
                            requiredRank,
                        )
                    }
                }
            }
        }

        // Is not full
        if (applicationConfig.enforceTaskParticipantLimitCheck) {
            if (task.participantLimit != null) {
                val actual =
                    taskMembershipRepository.countByTaskIdAndApproved(
                        task.id!!,
                        ApproveType.APPROVED,
                    )
                if (actual >= task.participantLimit!!)
                    return TaskParticipantsReachedLimitError(
                        task.id!!,
                        task.participantLimit!!,
                        actual,
                    )
            }
        }

        // Check team size for team tasks
        if (task.submitterType == TaskSubmitterType.TEAM) {
            val teamSize = teamService.getTeamSize(memberId)
            task.minTeamSize?.let { min ->
                if (teamSize < min) {
                    return TeamSizeNotEnoughError(teamSize, min)
                }
            }

            task.maxTeamSize?.let { max ->
                if (teamSize > max) {
                    return TeamSizeTooLargeError(teamSize, max)
                }
            }
        }

        return null
    }

    fun getJoinability(
        task: Task,
        userId: IdType,
    ): Triple<Boolean, List<TeamSummaryDTO>?, BaseError?> {
        return when (task.submitterType!!) {
            TaskSubmitterType.USER -> {
                val joinReject = isTaskJoinable(task, userId)
                Triple(joinReject == null, null, joinReject)
            }
            TaskSubmitterType.TEAM -> {
                val teams =
                    teamService.getTeamsThatUserCanUseToJoinTask(task.id!!, userId).filter {
                        isTaskJoinable(task, it.id) == null
                    }
                Triple(teams.isNotEmpty(), teams, null)
            }
        }
    }

    fun getSubmittability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberIdAndApproved(
                        task.id!!,
                        userId,
                        ApproveType.APPROVED,
                    ),
                    null,
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserCanUseToSubmitTask(task.id!!, userId)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getJoined(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberId(task.id!!, userId),
                    null,
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserJoinedTaskAs(task.id!!, userId)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getJoinedWithApproveType(
        task: Task,
        userId: IdType,
        approveType: ApproveType,
    ): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberIdAndApproved(
                        task.id!!,
                        userId,
                        approveType,
                    ),
                    null,
                )
            TaskSubmitterType.TEAM -> {
                val teams =
                    teamService.getTeamsThatUserJoinedTaskAsWithApprovedType(
                        task.id!!,
                        userId,
                        approveType,
                    )
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    /**
     * Gets the user-specific deadline for a task if they have joined it Returns null if the user
     * has not joined the task or has no specific deadline set
     */
    fun getUserDeadline(taskId: IdType, userId: IdType): Long? {
        // For user tasks, the user is the participant
        val participant = taskMembershipRepository.findByTaskIdAndMemberId(taskId, userId)
        if (participant.isPresent && participant.get().approved == ApproveType.APPROVED) {
            return participant.get().deadline?.let {
                it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        // For team tasks, need to find if any team that user belongs to has joined the task
        val task = taskRepository.findById(taskId).orElse(null) ?: return null
        if (task.submitterType == TaskSubmitterType.TEAM) {
            val teamsWithParticipation =
                teamService.getTeamsThatUserJoinedTaskAsWithApprovedType(
                    taskId,
                    userId,
                    ApproveType.APPROVED,
                )

            if (teamsWithParticipation.isNotEmpty()) {
                // Get the first team's deadline
                val teamId = teamsWithParticipation.first().id
                val teamParticipation =
                    taskMembershipRepository.findByTaskIdAndMemberId(taskId, teamId)
                if (teamParticipation.isPresent) {
                    return teamParticipation.get().deadline?.let {
                        it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                }
            }
        }

        return null
    }

    /**
     * 检查用户是否是任务的参与者
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 用户是否为任务参与者
     */
    fun isTaskParticipant(taskId: IdType, userId: IdType): Boolean {
        val task = getTask(taskId)

        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                taskMembershipRepository.existsByTaskIdAndMemberId(taskId, userId)

            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserJoinedTaskAs(taskId, userId)
                teams.isNotEmpty()
            }

            else -> false
        }
    }

    fun getUserParticipantId(taskId: IdType, userId: IdType): IdType? {
        return taskMembershipRepository
            .findByTaskIdAndMemberId(taskId, userId)
            .map { it.id!! }
            .orElse(null)
    }

    fun getTeamParticipantId(taskId: IdType, teamId: IdType): IdType? {
        return taskMembershipRepository
            .findByTaskIdAndMemberId(taskId, teamId)
            .map { it.id!! }
            .orElse(null)
    }

    fun getUserParticipationInfo(taskId: IdType, userId: IdType): TaskParticipationInfoDTO {
        val task = getTask(taskId)
        val identities = mutableListOf<TaskParticipationIdentityDTO>()

        when (task.submitterType!!) {
            TaskSubmitterType.USER -> {
                val membership = taskMembershipRepository.findByTaskIdAndMemberId(taskId, userId)
                if (membership.isPresent) {
                    identities.add(
                        TaskParticipationIdentityDTO(
                            id = membership.get().id!!,
                            type = TaskSubmitterTypeDTO.USER,
                            memberId = userId,
                            canSubmit = true,
                            approved = membership.get().approved!!.convert(),
                        )
                    )
                }
            }

            TaskSubmitterType.TEAM -> {
                teamService.getTeamsOfUser(userId)
                val userTeams = teamService.getTeamsOfUser(userId)
                for (team in userTeams) {
                    val teamId = team.id
                    val membership =
                        taskMembershipRepository.findByTaskIdAndMemberId(task.id!!, teamId)

                    if (membership.isPresent) {
                        val isAdmin = teamService.isTeamAtLeastAdmin(teamId, userId)
                        identities.add(
                            TaskParticipationIdentityDTO(
                                id = membership.get().id!!,
                                type = TaskSubmitterTypeDTO.TEAM,
                                memberId = teamId,
                                teamName = team.name,
                                canSubmit = isAdmin,
                                approved = membership.get().approved!!.convert(),
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
