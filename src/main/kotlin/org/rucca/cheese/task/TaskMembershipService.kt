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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
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
    private val logger = LoggerFactory.getLogger(javaClass)

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
                membership.deadline?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            approved = membership.approved!!.convert(),
            realNameInfo = realNameInfo,
            email = membership.email,
            phone = membership.phone,
            applyReason = membership.applyReason,
            personalAdvantage = membership.personalAdvantage,
            remark = membership.remark,
            teamMembers =
                if (task.submitterType == TaskSubmitterType.TEAM) {
                    getTeamParticipantMemberSummaries(task, membership)
                } else null,
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
                return null // No need to return a single real name info for team participants
            }

            else -> null
        }
    }

    private fun getRealNameInfoForTeamMember(
        membership: TaskMembership,
        memberId: IdType,
    ): TaskParticipantRealNameInfoDTO {
        return membership.teamMembersRealNameInfo
            .firstOrNull { it.memberId == memberId }
            ?.let { teamMember ->
                if (teamMember.realNameInfo.encrypted && membership.encryptionKeyId != null) {
                    decryptTeamMemberRealNameInfo(
                        teamMember.realNameInfo,
                        membership.encryptionKeyId!!,
                    )
                } else {
                    teamMember.realNameInfo.convert()
                }
            } ?: DefaultRealNameInfo.convert()
    }

    private fun encryptRealNameInfo(identity: UserIdentityDTO, taskKeyId: String): RealNameInfo {
        return RealNameInfo(
            realName = encryptionService.encryptData(identity.realName, taskKeyId),
            studentId = encryptionService.encryptData(identity.studentId, taskKeyId),
            grade = encryptionService.encryptData(identity.grade, taskKeyId),
            major = encryptionService.encryptData(identity.major, taskKeyId),
            className = encryptionService.encryptData(identity.className, taskKeyId),
            encrypted = true, // Mark as encrypted
        )
    }

    /**
     * Fixes missing real name information snapshots in TaskMemberships for a given task. It
     * retrieves the user's real name identity, encrypts it with the task's key, and saves it to the
     * membership record. This is an administrative function.
     *
     * @param taskId The ID of the task to fix.
     * @return The number of TaskMembership records updated.
     * @throws NotFoundError if the task with the given ID is not found.
     */
    @Transactional(propagation = Propagation.REQUIRED) // Ensure atomicity for the fix process
    fun fixRealNameInfoForTask(taskId: IdType): Int {
        logger.info("Starting real name info fix process for task ID: {}", taskId)
        val task = getTask(taskId)

        if (!task.requireRealName) {
            logger.warn("Task ID: {} does not require real name. No fix needed.", taskId)
            return 0
        }

        // Get or create the encryption key for this task
        val taskEncryptionKey = encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)
        val taskKeyId = taskEncryptionKey.id
        logger.debug("Using encryption key ID: {} for task ID: {}", taskKeyId, taskId)

        val memberships = taskMembershipRepository.findAllByTaskId(taskId)
        val updatedMemberships = mutableListOf<TaskMembership>()
        var updatedCount = 0

        logger.info("Found {} memberships for task ID: {}", memberships.size, taskId)

        for (membership in memberships) {
            var needsUpdate = false
            val memberId = membership.memberId ?: continue // Skip if memberId is somehow null

            try {
                if (!membership.isTeam) {
                    // --- Individual Participant ---
                    val userId = memberId
                    // Check if realNameInfo is missing or not properly set (e.g., not encrypted
                    // when it should be)
                    if (
                        membership.realNameInfo == null ||
                            membership.realNameInfo?.realName.isNullOrBlank() ||
                            !membership.realNameInfo!!.encrypted
                    ) {
                        logger.debug(
                            "Processing individual membership ID: {} for user ID: {}",
                            membership.id,
                            userId,
                        )
                        // Fetch user's real name identity (this decrypts it)
                        val userIdentity =
                            try {
                                userRealNameService.getUserIdentity(userId)
                            } catch (e: NotFoundError) {
                                logger.error(
                                    "User real name identity not found for user ID: {} (Membership ID: {}). Skipping this participant.",
                                    userId,
                                    membership.id,
                                )
                                null // Skip this participant
                            }

                        if (userIdentity != null) {
                            // Encrypt with task key and create the RealNameInfo snapshot
                            val encryptedInfo = encryptRealNameInfo(userIdentity, taskKeyId)
                            membership.realNameInfo = encryptedInfo
                            membership.encryptionKeyId = taskKeyId
                            needsUpdate = true
                            logger.info(
                                "Prepared update for individual membership ID: {} (User ID: {})",
                                membership.id,
                                userId,
                            )
                        }
                    } else {
                        logger.debug(
                            "Individual membership ID: {} already has valid real name info. Skipping.",
                            membership.id,
                        )
                    }
                } else {
                    // --- Team Participant ---
                    val teamId = memberId
                    // Check if team real name info is missing or seems incomplete
                    // A simple check is if it's empty, or if it exists but isn't marked encrypted
                    // (implies old data or error)
                    val requiresTeamFix =
                        membership.teamMembersRealNameInfo.isEmpty() ||
                            membership.teamMembersRealNameInfo.any { !it.realNameInfo.encrypted }

                    if (requiresTeamFix) {
                        logger.debug(
                            "Processing team membership ID: {} for team ID: {}",
                            membership.id,
                            teamId,
                        )
                        val (teamMembers, _) =
                            teamService.getTeamMembers(teamId) // Get current team members
                        if (teamMembers.isEmpty()) {
                            logger.warn(
                                "Team ID: {} has no members. Cannot fix real name info for membership ID: {}",
                                teamId,
                                membership.id,
                            )
                            continue // Skip if team has no members
                        }

                        val newTeamMembersRealNameInfo = mutableListOf<TeamMemberRealNameInfo>()
                        var teamFixPossible = true // Flag to track if all members can be processed

                        for (teamMember in teamMembers) {
                            val teamMemberUserId = teamMember.user.id
                            // Fetch team member's real name identity
                            val memberIdentity =
                                try {
                                    userRealNameService.getUserIdentity(teamMemberUserId)
                                } catch (e: NotFoundError) {
                                    logger.error(
                                        "User real name identity not found for team member ID: {} (Team ID: {}, Membership ID: {}). Skipping update for the entire team.",
                                        teamMemberUserId,
                                        teamId,
                                        membership.id,
                                    )
                                    teamFixPossible = false
                                    break // Stop processing this team if any member lacks info
                                }

                            val encryptedInfo = encryptRealNameInfo(memberIdentity, taskKeyId)
                            newTeamMembersRealNameInfo.add(
                                TeamMemberRealNameInfo(
                                    memberId = teamMemberUserId,
                                    realNameInfo = encryptedInfo,
                                )
                            )
                        }

                        // Only update if all members were processed successfully
                        if (teamFixPossible && newTeamMembersRealNameInfo.isNotEmpty()) {
                            // Replace the entire list with the newly generated one
                            membership.teamMembersRealNameInfo.clear()
                            membership.teamMembersRealNameInfo.addAll(newTeamMembersRealNameInfo)
                            membership.encryptionKeyId = taskKeyId
                            needsUpdate = true
                            logger.info(
                                "Prepared update for team membership ID: {} (Team ID: {}) with {} members",
                                membership.id,
                                teamId,
                                newTeamMembersRealNameInfo.size,
                            )
                        } else if (!teamFixPossible) {
                            logger.warn(
                                "Skipped update for team membership ID: {} due to missing real name info for one or more members.",
                                membership.id,
                            )
                        }
                    } else {
                        logger.debug(
                            "Team membership ID: {} already has valid real name info. Skipping.",
                            membership.id,
                        )
                    }
                }

                if (needsUpdate) {
                    updatedMemberships.add(membership)
                    updatedCount++
                }
            } catch (e: Exception) {
                // Catch unexpected errors during processing of a single membership
                logger.error(
                    "Unexpected error processing membership ID: {}: {}. Skipping this membership.",
                    membership.id,
                    e.message,
                    e,
                )
                // Continue to the next membership
            }
        } // End of loop through memberships

        if (updatedMemberships.isNotEmpty()) {
            logger.info(
                "Saving {} updated memberships for task ID: {}",
                updatedMemberships.size,
                taskId,
            )
            taskMembershipRepository.saveAll(updatedMemberships)
            logger.info("Successfully saved updated memberships.")
        } else {
            logger.info("No memberships required updates for task ID: {}", taskId)
        }

        return updatedCount
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
            when (task.submitterType) {
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
            when (task.submitterType) {
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

    private fun getTeamParticipantMemberSummaries(
        task: Task,
        membership: TaskMembership,
    ): List<TaskTeamParticipantMemberSummaryDTO> {
        val teamOwnerId = teamService.getTeamOwner(membership.memberId!!)
        return if (task.requireRealName) {
            // Return only the real name info for team members
            membership.teamMembersRealNameInfo.map { teamMember ->
                TaskTeamParticipantMemberSummaryDTO(
                    name = "",
                    intro = "",
                    avatarId = 1,
                    realNameInfo = getRealNameInfoForTeamMember(membership, teamMember.memberId),
                    isLeader = teamMember.memberId == teamOwnerId,
                )
            }
        } else {
            // Regular participant summary
            membership.teamMembersRealNameInfo.map { teamMember ->
                val user = userService.getUserDto(teamMember.memberId)
                TaskTeamParticipantMemberSummaryDTO(
                    name = user.username,
                    intro = user.intro,
                    avatarId = user.avatarId,
                    realNameInfo = null,
                    isLeader = teamMember.memberId == teamOwnerId,
                )
            }
        }
    }

    @Transactional(readOnly = true)
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

        val eligibility: EligibilityStatusDTO =
            when (task.submitterType) {
                TaskSubmitterType.USER -> checkUserEligibilityForUserTask(task, memberId)
                TaskSubmitterType.TEAM -> checkTeamEligibilityForTeamTask(task, memberId).first
            }
        if (!eligibility.eligible) {
            throw mapReasonToError(eligibility.reasons!!.first(), taskId, memberId)
        }

        var memberRealNameInfo: RealNameInfo? = null
        val isTeam = task.submitterType == TaskSubmitterType.TEAM
        val teamMembersRealNameInfo = mutableListOf<TeamMemberRealNameInfo>()

        // Process real name information
        if (task.requireRealName) {
            when (task.submitterType) {
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

    fun getSubmittability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType) {
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
        when (task.submitterType) {
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
        when (task.submitterType) {
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
            return participant
                .get()
                .deadline
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
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
                    return teamParticipation
                        .get()
                        .deadline
                        ?.atZone(ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli()
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

        when (task.submitterType) {
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

    /**
     * Safely extracts a value of the expected type from the details map. Returns a default value if
     * the key is not found or the type does not match.
     */
    private inline fun <reified T> getDetail(
        details: Map<String, Any>?,
        key: String,
        default: T,
    ): T {
        return details?.get(key) as? T ?: default
    }

    /**
     * Safely extracts a nullable value of the expected type from the details map. Returns null if
     * the key is not found or the type does not match.
     */
    private inline fun <reified T> getDetailNullable(details: Map<String, Any>?, key: String): T? {
        return details?.get(key) as? T
    }

    /**
     * Maps an EligibilityRejectReasonInfoDTO back to a specific BaseError subclass for
     * compatibility. This mapping is inherently lossy as BaseError constructors often require more
     * context than available solely within the reason DTO. It relies on conventions for keys within
     * the `reason.details` map.
     *
     * Assumed keys in `reason.details`:
     * - "userId": Long
     * - "teamId": Long
     * - "taskId": Long (Optional, can be passed as argument)
     * - "limit": Int
     * - "actual": Int
     * - "actualRank": Int
     * - "requiredRank": Int
     * - "actualSize": Int
     * - "requiredSize": Int
     * - "missingUserIds": List<Long>
     *
     * @param reason The eligibility rejection reason DTO.
     * @param contextTaskId Optional task ID from the calling context.
     * @param contextMemberId Optional member ID (user or team) from the calling context.
     * @return A specific BaseError subclass if possible, otherwise a ForbiddenError.
     */
    fun mapReasonToError(
        reason: EligibilityRejectReasonInfoDTO,
        contextTaskId: IdType? = null,
        contextMemberId: IdType? = null,
    ): BaseError {
        // Prioritize context IDs if available
        val taskId = contextTaskId ?: getDetail(reason.details, "taskId", 0L)
        val memberId =
            contextMemberId
                ?: getDetail(
                    reason.details,
                    "memberId",
                    0L,
                ) // Decide if memberId should be in details

        return when (reason.code) {
            EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING ->
                // AlreadyBeTaskParticipantError requires taskId and memberId.
                // If contextMemberId is not passed, it might be ambiguous (user vs team).
                // Using ForbiddenError might be safer if context is missing.
                if (taskId != 0L && memberId != 0L) {
                    AlreadyBeTaskParticipantError(taskId, memberId)
                } else {
                    ForbiddenError(
                        reason.message,
                        reason.details ?: emptyMap(),
                    ) // Fallback if context insufficient
                }

            EligibilityRejectReasonCodeDTO.PARTICIPANT_LIMIT_REACHED -> {
                val limit = getDetail(reason.details, "limit", 0)
                val actual = getDetail(reason.details, "actual", 0)
                // TaskParticipantsReachedLimitError needs taskId too.
                if (taskId != 0L) {
                    TaskParticipantsReachedLimitError(taskId, limit, actual)
                } else {
                    ForbiddenError(reason.message, reason.details ?: emptyMap())
                }
            }

            EligibilityRejectReasonCodeDTO.TASK_NOT_APPROVED,
            EligibilityRejectReasonCodeDTO.DEADLINE_PASSED,
            EligibilityRejectReasonCodeDTO.USER_ACCOUNT_ISSUE,
            EligibilityRejectReasonCodeDTO.INDIVIDUAL_PARTICIPATION_NOT_ALLOWED,
            EligibilityRejectReasonCodeDTO.TEAM_PARTICIPATION_NOT_ALLOWED ->
                // These map well to a general ForbiddenError
                ForbiddenError(reason.message, reason.details ?: emptyMap())

            EligibilityRejectReasonCodeDTO.USER_NOT_FOUND -> {
                val userId =
                    getDetail(
                        reason.details,
                        "userId",
                        memberId,
                    ) // Use memberId from context if detail missing
                NotFoundError("user", userId)
            }

            EligibilityRejectReasonCodeDTO.TEAM_NOT_FOUND -> {
                val teamId =
                    getDetail(
                        reason.details,
                        "teamId",
                        memberId,
                    ) // Use memberId from context if detail missing
                NotFoundError("team", teamId)
            }

            EligibilityRejectReasonCodeDTO.USER_RANK_NOT_HIGH_ENOUGH -> {
                val actual = getDetail(reason.details, "actualRank", 0)
                val required = getDetail(reason.details, "requiredRank", 0)
                YourRankIsNotHighEnoughError(actual, required)
            }

            EligibilityRejectReasonCodeDTO.USER_MISSING_REAL_NAME -> {
                val userId = getDetail(reason.details, "userId", memberId)
                RealNameInfoRequiredError(userId)
            }

            EligibilityRejectReasonCodeDTO.TEAM_SIZE_MIN_NOT_MET -> {
                val actual = getDetail(reason.details, "actualSize", 0)
                val required = getDetail(reason.details, "requiredSize", 0)
                TeamSizeNotEnoughError(actual, required)
            }

            EligibilityRejectReasonCodeDTO.TEAM_SIZE_MAX_EXCEEDED -> {
                val actual = getDetail(reason.details, "actualSize", 0)
                val required = getDetail(reason.details, "requiredSize", 0)
                TeamSizeTooLargeError(actual, required)
            }

            EligibilityRejectReasonCodeDTO.TEAM_MEMBER_MISSING_REAL_NAME -> {
                val missingIds = getDetailNullable<List<Long>>(reason.details, "missingUserIds")
                val firstMissingId = missingIds?.firstOrNull() ?: 0L

                RealNameInfoRequiredError(firstMissingId)
            }

            EligibilityRejectReasonCodeDTO.TEAM_MEMBER_RANK_NOT_HIGH_ENOUGH -> {
                val userId = getDetail(reason.details, "userId", 0L)
                val actual = getDetail(reason.details, "actualRank", 0)
                val required = getDetail(reason.details, "requiredRank", 0)
                YourTeamMemberRankIsNotHighEnoughError(userId, actual, required)
            }

            else -> ForbiddenError(reason.message, reason.details ?: emptyMap())
        }
    }

    // Helper to create reason DTO
    private fun createRejectReason(
        code: EligibilityRejectReasonCodeDTO,
        message: String,
        details: Map<String, Any>? = null,
    ): EligibilityRejectReasonInfoDTO {
        // You might want to fetch localized messages here in a real application
        return EligibilityRejectReasonInfoDTO(code = code, message = message, details = details)
    }

    // Helper to check rank (extracted logic)
    private fun checkRankEligibility(
        task: Task,
        userId: IdType,
        isUserTask: Boolean,
        reasons: MutableList<EligibilityRejectReasonInfoDTO>,
    ) {
        val needToCheckRank =
            applicationConfig.rankCheckEnforced && task.space.enableRank!! && task.rank != null
        if (needToCheckRank) {
            val requiredRank = task.rank!! - applicationConfig.rankJump
            val actualRank = spaceUserRankService.getRank(task.space.id!!, userId)
            if (actualRank < requiredRank) {
                if (isUserTask) {
                    reasons.add(
                        createRejectReason(
                            EligibilityRejectReasonCodeDTO.USER_RANK_NOT_HIGH_ENOUGH,
                            "Your rank ($actualRank) is not high enough. Required: $requiredRank.",
                            mapOf(
                                "userId" to userId,
                                "actualRank" to actualRank,
                                "requiredRank" to requiredRank,
                            ),
                        )
                    )
                } else {
                    val user = userService.getUserDto(userId) // Get user for message
                    reasons.add(
                        createRejectReason(
                            EligibilityRejectReasonCodeDTO.TEAM_MEMBER_RANK_NOT_HIGH_ENOUGH,
                            "Team member ${user.username}'s rank ($actualRank) is not high enough. Required: $requiredRank.",
                            mapOf(
                                "userId" to userId,
                                "actualRank" to actualRank,
                                "requiredRank" to requiredRank,
                            ),
                        )
                    )
                }
            }
        }
    }

    /** Checks basic user status (exists, not banned, etc.). Returns an EligibilityStatusDTO. */
    private fun checkUserBasicEligibility(userId: IdType): EligibilityStatusDTO {
        val reasons = mutableListOf<EligibilityRejectReasonInfoDTO>()
        try {
            // userService.getUser(userId) // Or a simpler check like
            // userService.checkUserActive(userId)
            if (!userService.existsUser(userId)) { // Assuming existsUser checks basic validity
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.USER_NOT_FOUND,
                        "User not found.",
                        mapOf("userId" to userId),
                    )
                )
            }
        } catch (e: Exception) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.USER_ACCOUNT_ISSUE,
                    "User account status check failed: ${e.message}",
                    mapOf<String, Any>(
                        "userId" to userId,
                        "error" to (e.message ?: "Unknown error"),
                    ),
                )
            )
        }
        return EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons)
    }

    /**
     * Checks eligibility for a specific USER to join a specific USER type task. Includes basic user
     * checks and task-specific constraints.
     */
    fun checkUserEligibilityForUserTask(task: Task, userId: IdType): EligibilityStatusDTO {
        // Start with basic user checks
        val basicEligibility = checkUserBasicEligibility(userId)
        if (!basicEligibility.eligible) {
            return basicEligibility // Return early if basic checks fail
        }

        val reasons =
            mutableListOf<EligibilityRejectReasonInfoDTO>() // Start fresh for task-specific reasons
        val taskId = task.id!!

        // --- Task-Specific Checks for USER joining USER task ---
        // 1. Task Approved
        if (task.approved != ApproveType.APPROVED) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TASK_NOT_APPROVED,
                    "Task is not approved yet.",
                    mapOf("taskId" to taskId, "userId" to userId),
                )
            )
        }

        // 2. Participant Limit (check against approved count)
        if (applicationConfig.enforceTaskParticipantLimitCheck && task.participantLimit != null) {
            val currentApprovedCount =
                taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
            if (currentApprovedCount >= task.participantLimit!!) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.PARTICIPANT_LIMIT_REACHED,
                        "Task participant limit (${task.participantLimit}) has been reached.",
                        mapOf(
                            "taskId" to taskId,
                            "userId" to userId,
                            "limit" to task.participantLimit!!,
                            "actual" to currentApprovedCount,
                        ),
                    )
                )
            }
        }

        // 3. Already Joined?
        if (taskMembershipRepository.existsByTaskIdAndMemberId(taskId, userId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING,
                    "You are already participating in this task.",
                    mapOf("taskId" to taskId, "userId" to userId),
                )
            )
        }

        // 4. Real Name Required?
        if (task.requireRealName) {
            try {
                userRealNameService.getUserIdentity(userId)
            } catch (e: NotFoundError) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.USER_MISSING_REAL_NAME,
                        "Your real name information is required for this task but is missing.",
                        mapOf("taskId" to taskId, "userId" to userId, "error" to e.message),
                    )
                )
            }
        }

        // 5. Rank Required?
        checkRankEligibility(task, userId, true, reasons) // isUserTask = true

        return EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons)
    }

    /**
     * Checks eligibility for a specific TEAM to join a specific TEAM type task. Includes team
     * checks (size, members' real names, members' ranks) and task constraints.
     */
    fun checkTeamEligibilityForTeamTask(
        task: Task,
        teamId: IdType,
    ): Triple<EligibilityStatusDTO, List<TeamMemberDTO>?, Boolean?> {
        val reasons = mutableListOf<EligibilityRejectReasonInfoDTO>()
        val taskId = task.id!!

        // --- Basic Team and Task Checks ---
        // 0. Task Approved?
        if (task.approved != ApproveType.APPROVED) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TASK_NOT_APPROVED,
                    "Task is not approved yet.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
        }

        // 1. Team Exists?
        if (!teamService.existsTeam(teamId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TEAM_NOT_FOUND,
                    "Team not found.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
            return Triple(
                EligibilityStatusDTO(eligible = false, reasons = reasons),
                null,
                null,
            ) // Stop if team doesn't exist
        }

        // 2. Participant Limit (check against approved count)
        if (applicationConfig.enforceTaskParticipantLimitCheck && task.participantLimit != null) {
            val currentApprovedCount =
                taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
            if (currentApprovedCount >= task.participantLimit!!) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.PARTICIPANT_LIMIT_REACHED,
                        "Task participant limit (${task.participantLimit}) has been reached.",
                        mapOf(
                            "taskId" to taskId,
                            "teamId" to teamId,
                            "limit" to task.participantLimit!!,
                            "actual" to currentApprovedCount,
                        ),
                    )
                )
            }
        }

        // 3. Team Already Joined?
        if (taskMembershipRepository.existsByTaskIdAndMemberId(taskId, teamId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING,
                    "This team is already participating in this task.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
        }

        // --- Team-Specific Checks ---
        // 4. Team Size
        val teamSize = teamService.getTeamSize(teamId) // Assumes team exists
        task.minTeamSize?.let { min ->
            if (teamSize < min) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.TEAM_SIZE_MIN_NOT_MET,
                        "Team size ($teamSize) is less than the required minimum ($min).",
                        mapOf(
                            "taskId" to taskId,
                            "teamId" to teamId,
                            "actualSize" to teamSize,
                            "requiredSize" to min,
                        ),
                    )
                )
            }
        }
        task.maxTeamSize?.let { max ->
            if (teamSize > max) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.TEAM_SIZE_MAX_EXCEEDED,
                        "Team size ($teamSize) exceeds the allowed maximum ($max).",
                        mapOf(
                            "taskId" to taskId,
                            "teamId" to teamId,
                            "actualSize" to teamSize,
                            "requiredSize" to max,
                        ),
                    )
                )
            }
        }

        val (memberDetails, allVerified) =
            teamService.getTeamMembers(teamId, queryRealNameStatus = task.requireRealName)

        // 5. Team Members' Real Name (if required)
        if (task.requireRealName) {
            if (allVerified != true) {
                val missingMembers =
                    memberDetails.filter { it.hasRealNameInfo != true }.map { it.user.id }
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.TEAM_MEMBER_MISSING_REAL_NAME,
                        "One or more team members are missing required real name information.",
                        mapOf(
                            "missingUserIds" to missingMembers,
                            "taskId" to taskId,
                            "teamId" to teamId,
                        ),
                    )
                )
            }
        }

        // 6. Team Members' Rank (if required)
        memberDetails.forEach { teamMember ->
            checkRankEligibility(task, teamMember.user.id, false, reasons) // isUserTask = false
        }

        return Triple(
            EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons),
            memberDetails,
            allVerified,
        )
    }

    /**
     * Calculates the detailed participation eligibility for the given user regarding the specified
     * task. Populates *either* userTaskStatus *or* teamTaskStatus in ParticipationEligibilityDTO
     * based strictly on the task's submitterType.
     *
     * @param task The task entity.
     * @param userId The ID of the user whose eligibility is being checked.
     * @return ParticipationEligibilityDTO containing detailed status for the relevant type.
     */
    fun getParticipationEligibility(task: Task, userId: IdType): ParticipationEligibilityDTO {
        return when (task.submitterType) {
            TaskSubmitterType.USER -> {
                // Calculate detailed user eligibility for this specific USER task
                val userStatus = checkUserEligibilityForUserTask(task, userId)
                ParticipationEligibilityDTO(user = userStatus, teams = null)
            }

            TaskSubmitterType.TEAM -> {
                // Calculate eligibility for each of the user's teams for this TEAM task
                val userTeams = teamService.getTeamsThatUserCanUseToJoinTask(task.id!!, userId)
                val teamsStatus =
                    userTeams.map { team ->
                        val (specificTeamEligibility, memberDetails, allVerified) =
                            checkTeamEligibilityForTeamTask(task, team.id)
                        TeamTaskEligibilityDTO(
                            team =
                                team.copy(
                                    allMembersVerified = allVerified,
                                    memberRealNameStatus =
                                        memberDetails?.map {
                                            TeamMemberRealNameStatusDTO(
                                                it.user.id,
                                                it.hasRealNameInfo == true,
                                                it.user.nickname,
                                            )
                                        },
                                ),
                            eligibility = specificTeamEligibility,
                        )
                    }
                ParticipationEligibilityDTO(user = null, teams = teamsStatus)
            }
        }
    }
}
