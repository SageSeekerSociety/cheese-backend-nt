package org.rucca.cheese.task.service

import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.SpaceUserRankService
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.error.*
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.services.UserRealNameService
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskMembershipEligibilityService(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskRepository: TaskRepository,
    private val userService: UserService,
    private val teamService: TeamService,
    private val userRealNameService: UserRealNameService,
    private val spaceUserRankService: SpaceUserRankService,
    private val applicationConfig: ApplicationConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Helper to get Task, potentially duplicated or accessed via core service later
    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    /** Checks eligibility for a USER to join a USER task */
    @Transactional(readOnly = true)
    fun checkUserEligibilityForUserTask(task: Task, userId: IdType): EligibilityStatusDTO {
        val basicEligibility = checkUserBasicEligibility(userId)
        if (!basicEligibility.eligible) return basicEligibility

        val reasons = mutableListOf<EligibilityRejectReasonInfoDTO>()
        val taskId = task.id!!

        if (task.submitterType != TaskSubmitterType.USER) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.INDIVIDUAL_PARTICIPATION_NOT_ALLOWED,
                    "This task only accepts team participation.",
                    mapOf("taskId" to taskId, "userId" to userId),
                )
            )
            return EligibilityStatusDTO(eligible = false, reasons = reasons)
        }

        if (task.approved != ApproveType.APPROVED) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TASK_NOT_APPROVED,
                    "Task is not approved yet.",
                    mapOf("taskId" to taskId, "userId" to userId),
                )
            )
        }

        if (applicationConfig.enforceTaskParticipantLimitCheck && task.participantLimit != null) {
            val currentApprovedCount =
                taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
            if (currentApprovedCount >= task.participantLimit!!) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.PARTICIPANT_LIMIT_REACHED,
                        "Task participant limit (${task.participantLimit}) reached.",
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

        if (taskMembershipRepository.existsByTaskIdAndMemberId(taskId, userId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING,
                    "You are already participating.",
                    mapOf("taskId" to taskId, "userId" to userId),
                )
            )
        }

        if (task.requireRealName) {
            if (!userRealNameService.hasUserIdentity(userId)) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.USER_MISSING_REAL_NAME,
                        "Real name information is required.",
                        mapOf("taskId" to taskId, "userId" to userId),
                    )
                )
            }
        }

        checkRankEligibility(task, userId, true, reasons) // isUserTaskContext = true

        return EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons)
    }

    /** Checks eligibility for a TEAM to join a TEAM task */
    @Transactional(readOnly = true)
    fun checkTeamEligibilityForTeamTask(
        task: Task,
        teamId: IdType,
    ): Triple<EligibilityStatusDTO, List<TeamMemberDTO>?, Boolean?> {
        val reasons = mutableListOf<EligibilityRejectReasonInfoDTO>()
        val taskId = task.id!!

        if (task.submitterType != TaskSubmitterType.TEAM) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TEAM_PARTICIPATION_NOT_ALLOWED,
                    "This task only accepts individual participation.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
            return Triple(EligibilityStatusDTO(eligible = false, reasons = reasons), null, null)
        }

        if (task.approved != ApproveType.APPROVED) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TASK_NOT_APPROVED,
                    "Task is not approved yet.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
        }

        if (!teamService.existsTeam(teamId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.TEAM_NOT_FOUND,
                    "Team not found.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
            return Triple(EligibilityStatusDTO(eligible = false, reasons = reasons), null, null)
        }

        if (applicationConfig.enforceTaskParticipantLimitCheck && task.participantLimit != null) {
            val currentApprovedCount =
                taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
            if (currentApprovedCount >= task.participantLimit!!) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.PARTICIPANT_LIMIT_REACHED,
                        "Task participant limit (${task.participantLimit}) reached.",
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

        if (taskMembershipRepository.existsByTaskIdAndMemberId(taskId, teamId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING,
                    "This team is already participating.",
                    mapOf("taskId" to taskId, "teamId" to teamId),
                )
            )
        }

        val (memberDetails, allVerified) =
            teamService.getTeamMembers(teamId, queryRealNameStatus = task.requireRealName)
        val teamSize = memberDetails.size

        task.minTeamSize?.let { min ->
            if (teamSize < min) {
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.TEAM_SIZE_MIN_NOT_MET,
                        "Team size ($teamSize) < minimum ($min).",
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
                        "Team size ($teamSize) > maximum ($max).",
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

        if (task.requireRealName) {
            if (allVerified != true) {
                val missingMembers =
                    memberDetails.filter { it.hasRealNameInfo != true }.map { it.user.id }
                reasons.add(
                    createRejectReason(
                        EligibilityRejectReasonCodeDTO.TEAM_MEMBER_MISSING_REAL_NAME,
                        "One or more team members missing real name info.",
                        mapOf(
                            "missingUserIds" to missingMembers,
                            "taskId" to taskId,
                            "teamId" to teamId,
                        ),
                    )
                )
            }
        }

        memberDetails.forEach { teamMember ->
            checkRankEligibility(
                task,
                teamMember.user.id,
                false,
                reasons,
            ) // isUserTaskContext = false
        }

        return Triple(
            EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons),
            memberDetails,
            allVerified,
        )
    }

    /** Gets overall participation eligibility for a user regarding a task */
    @Transactional(readOnly = true)
    fun getParticipationEligibility(task: Task, userId: IdType): ParticipationEligibilityDTO {
        return when (task.submitterType) {
            TaskSubmitterType.USER -> {
                val userStatus = checkUserEligibilityForUserTask(task, userId)
                ParticipationEligibilityDTO(user = userStatus, teams = null)
            }
            TaskSubmitterType.TEAM -> {
                val potentialTeams = teamService.getTeamsThatUserCanUseToJoinTask(task.id!!, userId)
                val teamsStatus =
                    potentialTeams.map { teamSummary ->
                        val (specificTeamEligibility, memberDetails, allVerified) =
                            checkTeamEligibilityForTeamTask(task, teamSummary.id)

                        val enhancedTeamSummary =
                            teamSummary.copy(
                                allMembersVerified = allVerified,
                                memberRealNameStatus =
                                    memberDetails?.map {
                                        TeamMemberRealNameStatusDTO(
                                            it.user.id,
                                            it.hasRealNameInfo == true,
                                            it.user.nickname,
                                        )
                                    },
                            )

                        TeamTaskEligibilityDTO(
                            team = enhancedTeamSummary,
                            eligibility = specificTeamEligibility,
                        )
                    }
                ParticipationEligibilityDTO(user = null, teams = teamsStatus)
            }
        }
    }

    /** Performs final compliance checks just before approving a membership. */
    fun performPreApprovalChecks(task: Task, memberId: IdType, isTeam: Boolean) {
        if (isTeam) {
            val (currentMembers, allVerified) =
                teamService.getTeamMembers(
                    teamId = memberId,
                    queryRealNameStatus = task.requireRealName,
                )
            val currentSize = currentMembers.size

            task.minTeamSize?.let { min ->
                if (currentSize < min) {
                    throw BadRequestError(
                        "Cannot approve: Team size ($currentSize) is below minimum ($min) at time of approval."
                    )
                }
            }
            task.maxTeamSize?.let { max ->
                if (currentSize > max) {
                    throw BadRequestError(
                        "Cannot approve: Team size ($currentSize) exceeds maximum ($max) at time of approval."
                    )
                }
            }

            if (task.requireRealName && allVerified != true) {
                val missingMembers =
                    currentMembers.filter { it.hasRealNameInfo != true }.map { it.user.id }
                throw BadRequestError(
                    "Cannot approve: Following team members lack real name info: ${missingMembers.joinToString()}"
                )
            }
        } else {
            if (task.requireRealName && !userRealNameService.hasUserIdentity(memberId)) {
                throw BadRequestError(
                    "Cannot approve: User $memberId is missing required real name information."
                )
            }
        }
        logger.debug("Pre-approval checks passed for task {} and member {}", task.id, memberId)
    }

    /** Ensures approving a participant won't exceed the limit */
    fun ensureTaskParticipantNotReachedLimit(taskId: IdType) {
        if (applicationConfig.enforceTaskParticipantLimitCheck) {
            val task = getTask(taskId) // Fetch task details
            task.participantLimit?.let { limit ->
                val actualApprovedCount =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actualApprovedCount >= limit) {
                    throw TaskParticipantsReachedLimitError(taskId, limit, actualApprovedCount)
                }
            }
        }
    }

    /** Automatically rejects pending participants if the approved count reaches the limit */
    @Transactional // Needs transaction as it modifies multiple entities
    fun autoRejectParticipantAfterReachesLimit(taskId: IdType) {
        if (applicationConfig.autoRejectParticipantAfterReachesLimit) {
            val task = getTask(taskId)
            task.participantLimit?.let { limit ->
                val actualApprovedCount =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actualApprovedCount >= limit) {
                    val participantsToReject =
                        taskMembershipRepository.findAllByTaskIdAndApproved(
                            taskId,
                            ApproveType.NONE,
                        )

                    if (participantsToReject.isNotEmpty()) {
                        logger.info(
                            "Task {} reached limit ({}), auto-rejecting {} pending participants.",
                            taskId,
                            limit,
                            participantsToReject.size,
                        )
                        participantsToReject.forEach {
                            it.approved = ApproveType.DISAPPROVED // Or REJECTED
                            it.rejectReason =
                                "Automatically rejected: Task participant limit reached."
                        }
                        taskMembershipRepository.saveAll(participantsToReject)
                    }
                }
            }
        }
    }

    /** Helper to check rank eligibility */
    private fun checkRankEligibility(
        task: Task,
        userId: IdType,
        isUserTaskContext: Boolean,
        reasons: MutableList<EligibilityRejectReasonInfoDTO>,
    ) {
        val rankConfig = applicationConfig.rankCheckEnforced
        val spaceConfig = task.space.enableRank ?: false
        val taskRankRequirement = task.rank

        if (rankConfig && spaceConfig && taskRankRequirement != null) {
            val requiredRank = taskRankRequirement - applicationConfig.rankJump
            if (requiredRank > 0) {
                val actualRank = spaceUserRankService.getRank(task.space.id!!, userId)
                if (actualRank < requiredRank) {
                    val reasonCode =
                        if (isUserTaskContext) {
                            EligibilityRejectReasonCodeDTO.USER_RANK_NOT_HIGH_ENOUGH
                        } else {
                            EligibilityRejectReasonCodeDTO.TEAM_MEMBER_RANK_NOT_HIGH_ENOUGH
                        }
                    val message =
                        if (isUserTaskContext) {
                            "Your rank ($actualRank) is not high enough. Required: $requiredRank."
                        } else {
                            val username = userService.getUserDto(userId).username
                            "Team member ${username}'s rank ($actualRank) is not high enough. Required: $requiredRank."
                        }
                    reasons.add(
                        createRejectReason(
                            reasonCode,
                            message,
                            mapOf(
                                "userId" to userId,
                                "actualRank" to actualRank,
                                "requiredRank" to requiredRank,
                                "taskId" to task.id!!,
                            ),
                        )
                    )
                }
            }
        }
    }

    /** Basic check if user exists and is active (simplified) */
    private fun checkUserBasicEligibility(userId: IdType): EligibilityStatusDTO {
        val reasons = mutableListOf<EligibilityRejectReasonInfoDTO>()
        if (!userService.existsUser(userId)) {
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.USER_NOT_FOUND,
                    "User not found or inactive.",
                    mapOf("userId" to userId),
                )
            )
        }
        return EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons)
    }

    /** Helper to map reason DTO back to specific errors */
    fun mapReasonToError(
        reason: EligibilityRejectReasonInfoDTO,
        contextTaskId: IdType? = null,
        contextMemberId: IdType? = null,
    ): BaseError {
        val taskId = contextTaskId ?: getDetail(reason.details, "taskId", 0L)
        val memberId = contextMemberId ?: getDetail(reason.details, "memberId", 0L)

        return when (reason.code) {
            EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING ->
                if (taskId != 0L && memberId != 0L) {
                    AlreadyBeTaskParticipantError(taskId, memberId)
                } else {
                    ForbiddenError(reason.message, reason.details ?: emptyMap())
                }
            EligibilityRejectReasonCodeDTO.PARTICIPANT_LIMIT_REACHED -> {
                val limit = getDetail(reason.details, "limit", 0)
                val actual = getDetail(reason.details, "actual", 0)
                if (taskId != 0L) {
                    TaskParticipantsReachedLimitError(taskId, limit, actual)
                } else {
                    ForbiddenError(reason.message, reason.details ?: emptyMap())
                }
            }
            EligibilityRejectReasonCodeDTO.USER_NOT_FOUND -> {
                val userId = getDetailNullable<Long>(reason.details, "userId") ?: memberId
                NotFoundError("user", userId)
            }
            EligibilityRejectReasonCodeDTO.TEAM_NOT_FOUND -> {
                val teamId = getDetailNullable<Long>(reason.details, "teamId") ?: memberId
                NotFoundError("team", teamId)
            }
            EligibilityRejectReasonCodeDTO.USER_RANK_NOT_HIGH_ENOUGH -> {
                val actual = getDetail(reason.details, "actualRank", 0)
                val required = getDetail(reason.details, "requiredRank", 0)
                YourRankIsNotHighEnoughError(actual, required)
            }
            EligibilityRejectReasonCodeDTO.USER_MISSING_REAL_NAME -> {
                val userId = getDetailNullable<Long>(reason.details, "userId") ?: memberId
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

    // --- Helper Methods ---
    private inline fun <reified T> getDetail(
        details: Map<String, Any>?,
        key: String,
        default: T,
    ): T {
        return details?.get(key) as? T ?: default
    }

    private inline fun <reified T> getDetailNullable(details: Map<String, Any>?, key: String): T? {
        return details?.get(key) as? T
    }

    private fun createRejectReason(
        code: EligibilityRejectReasonCodeDTO,
        message: String,
        details: Map<String, Any>? = null,
    ): EligibilityRejectReasonInfoDTO {
        return EligibilityRejectReasonInfoDTO(code = code, message = message, details = details)
    }
}
