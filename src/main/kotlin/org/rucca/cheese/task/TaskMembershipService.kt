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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// Default empty RealNameInfo when real name is not required or not available
val DefaultRealNameInfo =
    RealNameInfo(
        realName = null, // Use null for non-required fields
        studentId = null,
        grade = null,
        major = null,
        className = null,
        encrypted = false, // Explicitly mark as not encrypted
    )

fun RealNameInfo.convert(): TaskParticipantRealNameInfoDTO {
    return TaskParticipantRealNameInfoDTO(
        realName = realName ?: "", // Convert null to empty string for DTO if needed
        studentId = studentId ?: "",
        grade = grade ?: "",
        major = major ?: "",
        className = className ?: "",
    )
}

fun TaskParticipantRealNameInfoDTO.convert(): RealNameInfo {
    // This conversion might not be fully accurate if original was null vs empty string
    return RealNameInfo(
        realName = realName.ifBlank { null },
        studentId = studentId.ifBlank { null },
        grade = grade.ifBlank { null },
        major = major.ifBlank { null },
        className = className.ifBlank { null },
        encrypted = false, // Assume DTOs are decrypted
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

    @Transactional(readOnly = true)
    fun getTaskMembershipDTO(taskId: IdType, memberId: IdType): TaskMembershipDTO {
        val task = getTask(taskId)
        val membership = getTaskMembership(taskId, memberId)

        // Determine if we SHOULD attempt to display real name info based on task setting
        val shouldDisplayRealNameInfo = task.requireRealName

        // Get individual real name info (only applicable for USER tasks and if required)
        val individualRealNameInfo =
            if (task.submitterType == TaskSubmitterType.USER && shouldDisplayRealNameInfo) {
                getRealNameInfo(membership)
            } else null

        // Get team member summaries (always needed for TEAM tasks to display members)
        val teamMemberSummaries =
            if (task.submitterType == TaskSubmitterType.TEAM) {
                getTeamParticipantMemberSummaries(task, membership, shouldDisplayRealNameInfo)
            } else null

        // Get the main participant summary (User or Team)
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
            realNameInfo = individualRealNameInfo, // Only populated for USER task real name view
            email = membership.email,
            phone = membership.phone,
            applyReason = membership.applyReason,
            personalAdvantage = membership.personalAdvantage,
            remark = membership.remark,
            teamMembers = teamMemberSummaries, // Populated for TEAM tasks
        )
    }

    // Simplified: gets real name info only from the membership object itself
    private fun getRealNameInfo(membership: TaskMembership): TaskParticipantRealNameInfoDTO? {
        return membership.realNameInfo?.let { info ->
            if (info.encrypted && membership.encryptionKeyId != null) {
                decryptUserRealNameInfo(info, membership.encryptionKeyId!!)
            } else if (!info.encrypted && info.realName != null) { // Handle non-encrypted case
                info.convert()
            } else {
                null // No valid info or not encrypted when expected
            }
        }
    }

    // Decrypts real name info for a specific team member from the snapshot
    private fun getRealNameInfoForTeamMember(
        membership: TaskMembership, // Needed for encryptionKeyId
        memberSnapshot: TeamMemberRealNameInfo,
    ): TaskParticipantRealNameInfoDTO {
        val info = memberSnapshot.realNameInfo
        return if (info.encrypted && membership.encryptionKeyId != null) {
            decryptTeamMemberRealNameInfo(info, membership.encryptionKeyId!!)
        } else if (!info.encrypted && info.realName != null) { // Display non-encrypted if available
            info.convert()
        } else {
            // Fallback if encrypted but no key, or no data
            DefaultRealNameInfo.convert() // Return default empty DTO
        }
    }

    // Encrypts a UserIdentityDTO using the provided task key ID
    private fun encryptRealNameInfo(identity: UserIdentityDTO, taskKeyId: String): RealNameInfo {
        // Ensure data exists before encrypting, handle nulls if necessary based on UserIdentityDTO
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
     * Fixes missing real name information snapshots in TaskMemberships for a given task. IMPORTANT:
     * This function ONLY targets tasks where `requireRealName` is TRUE. It retrieves the user's
     * real name identity, encrypts it with the task's key, and saves it to the membership record.
     * This is an administrative function.
     */
    @Transactional(propagation = Propagation.REQUIRED) // Ensure atomicity for the fix process
    fun fixRealNameInfoForTask(taskId: IdType): Int {
        logger.info("Starting real name info fix process for task ID: {}", taskId)
        val task = getTask(taskId)

        // This function is only intended for tasks that REQUIRE real names
        if (!task.requireRealName) {
            logger.warn(
                "Task ID: {} does not require real name. Fix function is skipped as it's designed for required real name tasks.",
                taskId,
            )
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
                    // Check if realNameInfo is missing OR present but NOT marked as encrypted
                    if (membership.realNameInfo == null || !membership.realNameInfo!!.encrypted) {
                        logger.debug(
                            "Processing individual membership ID: {} for user ID: {} (Needs Fix)",
                            membership.id,
                            userId,
                        )
                        val userIdentity =
                            try {
                                userRealNameService.getUserIdentity(userId)
                            } catch (e: NotFoundError) {
                                logger.error(
                                    "User real name identity not found for user ID: {} (Membership ID: {}). Skipping fix for this participant.",
                                    userId,
                                    membership.id,
                                )
                                null // Skip this participant
                            }

                        if (userIdentity != null) {
                            val encryptedInfo = encryptRealNameInfo(userIdentity, taskKeyId)
                            membership.realNameInfo = encryptedInfo
                            membership.encryptionKeyId = taskKeyId // Ensure key ID is set
                            needsUpdate = true
                            logger.info(
                                "Prepared fix for individual membership ID: {} (User ID: {})",
                                membership.id,
                                userId,
                            )
                        }
                    } else {
                        logger.debug(
                            "Individual membership ID: {} already has encrypted real name info. Skipping fix.",
                            membership.id,
                        )
                    }
                } else {
                    // --- Team Participant ---
                    val teamId = memberId
                    // Check if snapshot is empty OR if ANY member's info is NOT marked as encrypted
                    val requiresTeamFix =
                        membership.teamMembersRealNameInfo.isEmpty() ||
                            membership.teamMembersRealNameInfo.any { !it.realNameInfo.encrypted }

                    if (requiresTeamFix) {
                        logger.debug(
                            "Processing team membership ID: {} for team ID: {} (Needs Fix)",
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
                            val memberIdentity =
                                try {
                                    userRealNameService.getUserIdentity(teamMemberUserId)
                                } catch (e: NotFoundError) {
                                    logger.error(
                                        "User real name identity not found for team member ID: {} (Team ID: {}, Membership ID: {}). Skipping fix for the entire team.",
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

                        if (teamFixPossible && newTeamMembersRealNameInfo.isNotEmpty()) {
                            membership.teamMembersRealNameInfo.clear()
                            membership.teamMembersRealNameInfo.addAll(newTeamMembersRealNameInfo)
                            membership.encryptionKeyId = taskKeyId // Ensure key ID is set
                            needsUpdate = true
                            logger.info(
                                "Prepared fix for team membership ID: {} (Team ID: {}) with {} members",
                                membership.id,
                                teamId,
                                newTeamMembersRealNameInfo.size,
                            )
                        } else if (!teamFixPossible) {
                            logger.warn(
                                "Skipped fix for team membership ID: {} due to missing real name info for one or more members.",
                                membership.id,
                            )
                        }
                    } else {
                        logger.debug(
                            "Team membership ID: {} already has encrypted real name info for all members. Skipping fix.",
                            membership.id,
                        )
                    }
                }

                if (needsUpdate) {
                    updatedMemberships.add(membership)
                    updatedCount++
                }
            } catch (e: Exception) {
                logger.error(
                    "Unexpected error processing membership ID: {}: {}. Skipping this membership.",
                    membership.id,
                    e.message,
                    e,
                )
            }
        } // End of loop

        if (updatedMemberships.isNotEmpty()) {
            logger.info(
                "Saving {} fixed memberships for task ID: {}",
                updatedMemberships.size,
                taskId,
            )
            taskMembershipRepository.saveAll(updatedMemberships)
            logger.info("Successfully saved fixed memberships.")
        } else {
            logger.info("No memberships required fixing for task ID: {}", taskId)
        }

        return updatedCount
    }

    // Decrypts RealNameInfo assuming it's marked encrypted and keyId is valid
    private fun decryptUserRealNameInfo(
        encryptedInfo: RealNameInfo,
        keyId: String,
    ): TaskParticipantRealNameInfoDTO {
        // Assume fields are non-null if encrypted=true, handle potential nulls defensively if
        // needed
        return RealNameInfo(
                realName = encryptionService.decryptData(encryptedInfo.realName!!, keyId),
                studentId = encryptionService.decryptData(encryptedInfo.studentId!!, keyId),
                grade = encryptionService.decryptData(encryptedInfo.grade!!, keyId),
                major = encryptionService.decryptData(encryptedInfo.major!!, keyId),
                className = encryptionService.decryptData(encryptedInfo.className!!, keyId),
                encrypted = false, // Result is decrypted
            )
            .convert()
    }

    // Decrypts RealNameInfo for a team member
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
                encrypted = false, // Result is decrypted
            )
            .convert()
    }

    // Gets the summary DTO for the main participant (User or Team)
    private fun getParticipantSummary(
        task: Task,
        membership: TaskMembership,
        displayRealName: Boolean, // Whether to show real name (implies task requires it)
    ): TaskParticipantSummaryDTO {
        val memberId = membership.memberId!!
        return when (task.submitterType) {
            TaskSubmitterType.USER -> {
                if (displayRealName) {
                    // Minimal info, real name comes from separate field in TaskMembershipDTO
                    TaskParticipantSummaryDTO(
                        id = membership.id!!,
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
                    )
                }
            }
            TaskSubmitterType.TEAM -> {
                // Always show team info, real names are in the members list
                val team = teamService.getTeamDto(memberId)
                TaskParticipantSummaryDTO(
                    id = team.id,
                    intro =
                        if (displayRealName) ""
                        else team.intro, // Show intro only if not in real name mode
                    name = team.name, // Always show team name
                    avatarId = team.avatarId,
                    participantId = membership.participantUuid,
                )
            }
        }
    }

    // Gets summaries for each member within a team participation snapshot
    private fun getTeamParticipantMemberSummaries(
        task: Task,
        membership: TaskMembership,
        displayRealName: Boolean, // Whether to display real names instead of usernames
    ): List<TaskTeamParticipantMemberSummaryDTO> {
        if (membership.teamMembersRealNameInfo.isEmpty()) {
            logger.warn(
                "Team membership ID {} for task {} has an empty team member snapshot!",
                membership.id,
                task.id,
            )
            return emptyList() // Should not happen if snapshot is always created
        }

        val teamOwnerId = teamService.getTeamOwner(membership.memberId!!)

        return membership.teamMembersRealNameInfo.map { memberSnapshot ->
            val memberUserId = memberSnapshot.memberId
            val isLeader = memberUserId == teamOwnerId

            if (displayRealName) {
                // Display real name info from the snapshot
                TaskTeamParticipantMemberSummaryDTO(
                    name = "",
                    intro = "",
                    avatarId = 1,
                    isLeader = isLeader,
                    participantMemberId = memberSnapshot.participantMemberUuid,
                    realNameInfo = getRealNameInfoForTeamMember(membership, memberSnapshot),
                )
            } else {
                // Display regular user info (username, avatar, intro)
                val user = userService.getUserDto(memberUserId)
                TaskTeamParticipantMemberSummaryDTO(
                    name = user.username,
                    intro = user.intro,
                    avatarId = user.avatarId,
                    userId = user.id,
                    isLeader = isLeader,
                    participantMemberId = memberSnapshot.participantMemberUuid,
                    realNameInfo = null,
                )
            }
        }
    }

    @Transactional(readOnly = true)
    fun getTaskMembershipDTOs(
        taskId: IdType,
        approveType: ApproveType?,
        queryRealNameInfo: Boolean =
            false, // Note: This flag is now less relevant as display logic is based on
        // task.requireRealName
    ): List<TaskMembershipDTO> {
        val task = getTask(taskId) // Fetch task once
        val shouldDisplayRealNameInfo =
            task.requireRealName // Determine display mode based on task setting

        // 1. Fetch all relevant TaskMembership entities
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(TaskMembership::class.java)
        val root = cq.from(TaskMembership::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<Task>("task").get<IdType>("id"), taskId))
        approveType?.let { predicates.add(cb.equal(root.get<ApproveType>("approved"), it)) }
        cq.where(*predicates.toTypedArray())
        // Consider adding fetch joins if Task or other direct relations are needed often,
        // but be careful as it can increase initial query load.
        // cq.select(root).distinct(true)
        // root.fetch<Any, Any>("task", jakarta.persistence.criteria.JoinType.LEFT) // Example fetch
        // join

        val participants = entityManager.createQuery(cq).resultList
        if (participants.isEmpty()) {
            return emptyList()
        }

        // 2. Bulk fetch required User/Team DTOs for the main participants
        val userMemberIds = participants.filter { !it.isTeam }.mapNotNull { it.memberId }
        val teamMemberIds = participants.filter { it.isTeam }.mapNotNull { it.memberId }

        val userDtoMap =
            if (userMemberIds.isNotEmpty()) userService.getUserDtos(userMemberIds) else emptyMap()
        val teamDtoMap =
            if (teamMemberIds.isNotEmpty()) teamService.getTeamDtos(teamMemberIds) else emptyMap()

        // 3. Bulk fetch required User DTOs for ALL team members across all team memberships
        val allTeamMemberUserIds =
            participants
                .filter { it.isTeam }
                .flatMap { it.teamMembersRealNameInfo }
                .map { it.memberId }
                .distinct() // Get unique user IDs across all teams

        val teamMemberUserDtoMap =
            if (allTeamMemberUserIds.isNotEmpty()) userService.getUserDtos(allTeamMemberUserIds)
            else emptyMap()

        // 4. Fetch team owner IDs (can potentially be optimized further if TeamDTO includes
        // ownerId)
        val teamOwnerIdMap =
            teamMemberIds.associateWith { teamId ->
                try {
                    teamService.getTeamOwner(teamId)
                } catch (e: NotFoundError) {
                    null
                }
            }

        // 5. Map results in memory using pre-fetched data
        return participants.map { membership ->
            val memberId = membership.memberId!!
            val dbMembershipId = membership.id!!
            val participantUuid = membership.participantUuid
            val isTeam = membership.isTeam

            // Build participant summary using maps
            val participantSummary =
                if (isTeam) {
                    val teamDto =
                        teamDtoMap[memberId]
                            ?: throw IllegalStateException(
                                "TeamDTO not found for ID: $memberId"
                            ) // Should exist
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
                            ?: throw IllegalStateException(
                                "UserDTO not found for ID: $memberId"
                            ) // Should exist
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

            // Build team member summaries using maps (if applicable)
            val teamMemberSummaries: List<TaskTeamParticipantMemberSummaryDTO>? =
                if (isTeam) {
                    val teamOwnerId = teamOwnerIdMap[memberId] // Get pre-fetched owner ID
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
                                realNameInfo =
                                    getRealNameInfoForTeamMember(membership, memberSnapshot),
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
                } else {
                    null
                }

            // Build individual real name info (if applicable)
            val individualRealNameInfo =
                if (!isTeam && shouldDisplayRealNameInfo) {
                    getRealNameInfo(membership)
                } else {
                    null
                }

            // Construct the final DTO
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
                realNameInfo = individualRealNameInfo, // Populated correctly now
                email = membership.email,
                phone = membership.phone,
                applyReason = membership.applyReason,
                personalAdvantage = membership.personalAdvantage,
                remark = membership.remark,
                teamMembers = teamMemberSummaries, // Populated correctly now
            )
        }
    }

    @Transactional // Ensure atomicity for creating membership and potentially fetching/encrypting
    // data
    fun addTaskParticipant(
        taskId: IdType,
        memberId: IdType, // userId for USER task, teamId for TEAM task
        deadline: LocalDateTime?,
        approved: ApproveType,
        email: String?,
        phone: String?,
        applyReason: String?,
        personalAdvantage: String?,
        remark: String?,
    ): TaskMembershipDTO {
        // Basic contact info check
        if (email.isNullOrBlank() && phone.isNullOrBlank()) {
            throw EmailOrPhoneRequiredError(taskId, memberId)
        }

        val task = getTask(taskId)
        val isTeam = task.submitterType == TaskSubmitterType.TEAM

        // Check eligibility BEFORE proceeding
        val eligibility: EligibilityStatusDTO
        var currentTeamMembers: List<TeamMemberDTO>? = null // Store team members if fetched

        when (task.submitterType) {
            TaskSubmitterType.USER -> {
                eligibility = checkUserEligibilityForUserTask(task, memberId)
            }
            TaskSubmitterType.TEAM -> {
                val (teamEligibility, fetchedMembers, _) =
                    checkTeamEligibilityForTeamTask(task, memberId)
                eligibility = teamEligibility
                currentTeamMembers = fetchedMembers // Store fetched members for snapshot
            }
        }

        if (!eligibility.eligible) {
            // Map the first reason to an error and throw it
            throw mapReasonToError(eligibility.reasons!!.first(), taskId, memberId)
        }

        // --- Prepare Snapshot Information ---
        var individualRealNameSnapshot: RealNameInfo? = null // For USER task
        val teamMemberSnapshotList = mutableListOf<TeamMemberRealNameInfo>() // For TEAM task
        var actualEncryptionHappened = false // Track if any data was actually encrypted
        var encryptionKeyIdToSave: String? = null // Store key ID only if encryption happened

        if (isTeam) {
            // --- TEAM: Always create snapshot ---
            individualRealNameSnapshot = null // Not used for team's main RealNameInfo field

            // Get encryption key ONLY if needed
            val taskKey =
                if (task.requireRealName) {
                    encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)
                } else {
                    null
                }
            encryptionKeyIdToSave = taskKey?.id // Store key ID IF we got one

            // Use pre-fetched team members if available from eligibility check
            val teamMembersToSnapshot =
                currentTeamMembers
                    ?: teamService.getTeamMembers(memberId).first // Fetch again if not available

            if (
                teamMembersToSnapshot.isEmpty() &&
                    task.minTeamSize != null &&
                    task.minTeamSize!! > 0
            ) {
                // This case should ideally be caught by eligibility check, but double-check
                throw TeamSizeNotEnoughError(0, task.minTeamSize!!)
            }

            for (teamMember in teamMembersToSnapshot) {
                val currentMemberUserId = teamMember.user.id
                val memberRealNameInfoForSnapshot: RealNameInfo

                if (task.requireRealName) {
                    // Requirement: Fetch, encrypt, and store real name info
                    try {
                        val userIdentity = userRealNameService.getUserIdentity(currentMemberUserId)
                        if (taskKey == null)
                            throw IllegalStateException(
                                "Encryption key missing for required real name task."
                            )
                        memberRealNameInfoForSnapshot =
                            encryptRealNameInfo(userIdentity, taskKey.id)
                        actualEncryptionHappened = true
                    } catch (e: NotFoundError) {
                        // Should have been caught by eligibility check, but throw defensively
                        throw RealNameInfoRequiredError(currentMemberUserId)
                    }
                } else {
                    // No requirement: Store default, non-encrypted info
                    memberRealNameInfoForSnapshot = DefaultRealNameInfo
                }

                teamMemberSnapshotList.add(
                    TeamMemberRealNameInfo(
                        memberId = currentMemberUserId,
                        realNameInfo = memberRealNameInfoForSnapshot,
                    )
                )
            } // End loop through team members

            // Final check on snapshot size against task constraints
            val snapshotSize = teamMemberSnapshotList.size
            task.minTeamSize?.let { min ->
                if (snapshotSize < min) {
                    // Should be caught by eligibility, but good to have defensive check
                    throw TeamSizeNotEnoughError(snapshotSize, min)
                }
            }
            task.maxTeamSize?.let { max ->
                if (snapshotSize > max) {
                    throw TeamSizeTooLargeError(snapshotSize, max)
                }
            }
        } else {
            // --- INDIVIDUAL ---
            if (task.requireRealName) {
                // Requirement: Fetch, encrypt, and store real name info
                try {
                    val userIdentity =
                        userRealNameService.getUserIdentity(memberId) // memberId is userId
                    val taskKey =
                        encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)
                    individualRealNameSnapshot = encryptRealNameInfo(userIdentity, taskKey.id)
                    actualEncryptionHappened = true
                    encryptionKeyIdToSave = taskKey.id
                } catch (e: NotFoundError) {
                    // Should have been caught by eligibility check
                    throw RealNameInfoRequiredError(memberId)
                }
            } else {
                // No requirement: Store null or default
                individualRealNameSnapshot = null // Or DefaultRealNameInfo based on DB constraints
            }
        }

        // --- Create and Save TaskMembership ---
        val membership =
            TaskMembership(
                task = task,
                memberId = memberId,
                deadline = deadline,
                approved = approved,
                isTeam = isTeam,
                realNameInfo = individualRealNameSnapshot, // Null for teams
                teamMembersRealNameInfo = teamMemberSnapshotList, // Populated for teams
                email = email ?: "", // Store non-null empty string if null passed
                phone = phone ?: "", // Store non-null empty string if null passed
                applyReason = applyReason ?: "",
                personalAdvantage = personalAdvantage ?: "",
                remark = remark ?: "",
                encryptionKeyId =
                    if (actualEncryptionHappened) encryptionKeyIdToSave
                    else null, // Only save key ID if encryption occurred
            )

        val savedMembership = taskMembershipRepository.save(membership)
        logger.info(
            "Created TaskMembership ID: {} for {} ID: {}",
            savedMembership.id,
            if (isTeam) "Team" else "User",
            memberId,
        )

        // Perform post-save actions like auto-rejecting others if limit reached
        autoRejectParticipantAfterReachesLimit(taskId)

        // Return DTO, querying real name info based on task setting
        return getTaskMembershipDTO(taskId, savedMembership.memberId!!)
    }

    @Transactional
    fun updateTaskMembership(
        taskId: IdType,
        memberId: IdType,
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO,
    ): TaskMembershipDTO {
        val participant = getTaskMembership(taskId, memberId)
        val task = participant.task!! // Assume task is loaded or fetch if needed

        // Check participant limit before approving
        if (
            patchTaskMembershipRequestDTO.approved == ApproveTypeDTO.APPROVED &&
                participant.approved != ApproveType.APPROVED
        ) {
            ensureTaskParticipantNotReachedLimit(taskId)
            // Add final pre-approval checks here if needed (see previous discussion)
            // e.g., re-validate team size, member real names if task has LOCK_ON_APPROVAL policy
        }

        // Prevent setting deadline unless approved (or becoming approved in this patch)
        if (
            patchTaskMembershipRequestDTO.deadline != null &&
                participant.approved != ApproveType.APPROVED &&
                patchTaskMembershipRequestDTO.approved != ApproveTypeDTO.APPROVED
        ) {
            throw ForbiddenError(
                "Cannot set deadline for non-approved task membership",
                mapOf("taskId" to task.id!!, "participantId" to participant.id!!),
            )
        }

        val previousApprovedStatus = participant.approved

        entityPatcher.patch(participant, patchTaskMembershipRequestDTO) {
            handle(PatchTaskMembershipRequestDTO::deadline) { entity, value ->
                entity.deadline = value.toLocalDateTime()
            }
            handle(PatchTaskMembershipRequestDTO::approved) { entity, value ->
                entity.approved = value.convert()
            }
            // Add handlers for other patchable fields like rejectReason if applicable
        }

        val savedParticipant = taskMembershipRepository.save(participant)

        // Auto-reject others ONLY if the status changed TO approved in this update
        if (
            savedParticipant.approved == ApproveType.APPROVED &&
                previousApprovedStatus != ApproveType.APPROVED
        ) {
            autoRejectParticipantAfterReachesLimit(taskId)
        }

        // Return DTO, respecting task's real name requirement for display
        return getTaskMembershipDTO(taskId, memberId)
    }

    @Transactional
    fun updateTaskMembership(
        participantId: IdType,
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO,
    ): TaskMembershipDTO {
        val participant = getTaskMembership(participantId)
        val task = participant.task!! // Assume task is loaded
        val memberId = participant.memberId!!

        // Check participant limit before approving
        if (
            patchTaskMembershipRequestDTO.approved == ApproveTypeDTO.APPROVED &&
                participant.approved != ApproveType.APPROVED
        ) {
            ensureTaskParticipantNotReachedLimit(task.id!!)
            // Add final pre-approval checks here if needed
        }

        // Prevent setting deadline unless approved
        if (
            patchTaskMembershipRequestDTO.deadline != null &&
                participant.approved != ApproveType.APPROVED &&
                patchTaskMembershipRequestDTO.approved != ApproveTypeDTO.APPROVED
        ) {
            throw ForbiddenError(
                "Cannot set deadline for non-approved task membership",
                mapOf("taskId" to task.id!!, "participantId" to participantId),
            )
        }

        val previousApprovedStatus = participant.approved

        entityPatcher.patch(participant, patchTaskMembershipRequestDTO) {
            handle(PatchTaskMembershipRequestDTO::deadline) { entity, value ->
                entity.deadline = value.toLocalDateTime()
            }
            handle(PatchTaskMembershipRequestDTO::approved) { entity, value ->
                entity.approved = value.convert()
            }
            // Add handlers for other patchable fields
        }

        val savedParticipant = taskMembershipRepository.save(participant)

        // Auto-reject others ONLY if the status changed TO approved
        if (
            savedParticipant.approved == ApproveType.APPROVED &&
                previousApprovedStatus != ApproveType.APPROVED
        ) {
            autoRejectParticipantAfterReachesLimit(task.id!!)
        }

        // Return DTO, respecting task's real name requirement for display
        return getTaskMembershipDTO(task.id!!, memberId)
    }

    // Ensures approving a participant won't exceed the limit
    fun ensureTaskParticipantNotReachedLimit(taskId: IdType) {
        if (applicationConfig.enforceTaskParticipantLimitCheck) {
            val task = getTask(taskId) // Fetch task details
            task.participantLimit?.let { limit -> // Check only if limit is set
                val actualApprovedCount =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actualApprovedCount >= limit) {
                    throw TaskParticipantsReachedLimitError(taskId, limit, actualApprovedCount)
                }
            }
        }
    }

    // Automatically rejects pending participants if the approved count reaches the limit
    @Transactional // Needs transaction as it modifies multiple entities
    fun autoRejectParticipantAfterReachesLimit(taskId: IdType) {
        if (applicationConfig.autoRejectParticipantAfterReachesLimit) {
            val task = getTask(taskId)
            task.participantLimit?.let { limit -> // Check only if limit is set
                val actualApprovedCount =
                    taskMembershipRepository.countByTaskIdAndApproved(taskId, ApproveType.APPROVED)
                if (actualApprovedCount >= limit) {
                    // Find participants currently pending (ApproveType.NONE)
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
                                "Automatically rejected: Task participant limit reached." // Add
                            // reason
                        }
                        taskMembershipRepository.saveAll(participantsToReject)
                    }
                }
            }
        }
    }

    // Soft delete a task participant by their membership ID
    @Transactional
    fun removeTaskParticipant(
        taskId: IdType,
        participantId: IdType,
    ) { // taskId might be redundant if participantId is unique
        val participant = getTaskMembership(participantId)
        // Optional: Check if participant belongs to the given taskId if needed for authorization
        // if (participant.task?.id != taskId) { throw SomePermissionError }
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
        logger.info("Soft deleted TaskMembership ID: {}", participantId)
        // Consider soft-deleting related submissions? Depends on requirements.
    }

    // Soft delete a task participant by member ID (User or Team)
    @Transactional
    fun removeTaskParticipantByMemberId(taskId: IdType, memberId: IdType) {
        val participant = getTaskMembership(taskId, memberId) // Finds the specific membership
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
        logger.info("Soft deleted TaskMembership for task {} and member {}", taskId, memberId)
    }

    // --- Eligibility, Submittability, Joined Checks ---
    // These methods seem okay, but ensure they correctly handle team logic vs user logic

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
                // A user can submit IF they are an admin/owner of ANY team that is APPROVED for the
                // task
                val teams = teamService.getTeamsThatUserCanUseToSubmitTask(task.id!!, userId)
                Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getJoined(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                Pair(taskMembershipRepository.existsByTaskIdAndMemberId(task.id!!, userId), null)

            TaskSubmitterType.TEAM -> {
                // User has joined IF they are a member of ANY team participating (regardless of
                // approval status)
                val teams = teamService.getTeamsThatUserJoinedTaskAs(task.id!!, userId)
                Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getJoinedWithApproveType(
        task: Task,
        userId: IdType,
        approveType: ApproveType,
    ): Pair<Boolean, List<TeamSummaryDTO>?> {
        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                Pair(
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
                Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    /**
     * Gets the user-specific deadline for a task. For TEAM tasks, it finds the deadline of the
     * first APPROVED team the user is part of for that task.
     */
    @Transactional(readOnly = true)
    fun getUserDeadline(taskId: IdType, userId: IdType): Long? {
        val task = getTask(taskId) // Fetch task once

        val membership =
            when (task.submitterType) {
                TaskSubmitterType.USER -> {
                    taskMembershipRepository.findByTaskIdAndMemberId(taskId, userId).orElse(null)
                }
                TaskSubmitterType.TEAM -> {
                    // Find the first *approved* team membership the user is part of for this task
                    teamService
                        .getTeamsThatUserJoinedTaskAsWithApprovedType(
                            taskId,
                            userId,
                            ApproveType.APPROVED,
                        )
                        .firstOrNull() // Get the first eligible team DTO
                        ?.let { team ->
                            taskMembershipRepository
                                .findByTaskIdAndMemberId(taskId, team.id)
                                .orElse(null)
                        } // Find its membership
                }
            }

        // Return deadline only if membership exists and is approved
        return membership
            ?.takeIf { it.approved == ApproveType.APPROVED }
            ?.deadline
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
    }

    /** Checks if the user is considered a participant in the task (directly or via a team). */
    @Transactional(readOnly = true)
    fun isTaskParticipant(taskId: IdType, userId: IdType): Boolean {
        val task = getTask(taskId)
        return when (task.submitterType) {
            TaskSubmitterType.USER ->
                taskMembershipRepository.existsByTaskIdAndMemberId(taskId, userId)
            TaskSubmitterType.TEAM -> {
                // Check if the user is a member of any team that has a TaskMembership for this task
                teamService.getTeamsThatUserJoinedTaskAs(taskId, userId).isNotEmpty()
            }
        }
    }

    // Get membership ID for a specific user participant
    @Transactional(readOnly = true)
    fun getUserParticipantId(taskId: IdType, userId: IdType): IdType? {
        return taskMembershipRepository
            .findByTaskIdAndMemberId(
                taskId,
                userId,
            ) // Assumes USER task or finds the direct record if misused
            .filter { !it.isTeam } // Ensure it's not a team membership record
            .map { it.id!! }
            .orElse(null)
    }

    // Get membership ID for a specific team participant
    @Transactional(readOnly = true)
    fun getTeamParticipantId(taskId: IdType, teamId: IdType): IdType? {
        return taskMembershipRepository
            .findByTaskIdAndMemberId(taskId, teamId)
            .filter { it.isTeam } // Ensure it's a team membership record
            .map { it.id!! }
            .orElse(null)
    }

    // Provides info about how a user is participating (directly or via which teams)
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
                            // User can always submit if they have an approved membership for a USER
                            // task
                            canSubmit = membership.approved == ApproveType.APPROVED,
                            approved = membership.approved!!.convert(),
                        )
                    )
                }
            }
            TaskSubmitterType.TEAM -> {
                // Find all teams the user is part of
                val userTeams =
                    teamService.getTeamsOfUser(userId) // Maybe optimize to get only relevant teams
                for (team in userTeams) {
                    val teamId = team.id
                    // Check if *this specific team* has a membership for the task
                    taskMembershipRepository.findByTaskIdAndMemberId(task.id!!, teamId).ifPresent {
                        membership ->
                        // Check if the user can submit on behalf of *this* team (is admin/owner)
                        val canSubmitForThisTeam =
                            teamService.isTeamAtLeastAdmin(teamId, userId) &&
                                membership.approved == ApproveType.APPROVED

                        identities.add(
                            TaskParticipationIdentityDTO(
                                id = membership.id!!,
                                type = TaskSubmitterTypeDTO.TEAM,
                                memberId = teamId,
                                teamName = team.name, // Use name from TeamSummaryDTO
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

    // --- Eligibility Check Logic ---
    // (Keeping the existing eligibility check logic, assuming it's functional)

    // Helper to map reason DTO back to specific errors (potentially lossy)
    fun mapReasonToError(
        reason: EligibilityRejectReasonInfoDTO,
        contextTaskId: IdType? = null,
        contextMemberId: IdType? = null, // User or Team ID
    ): BaseError {
        // Prioritize context IDs if available
        val taskId = contextTaskId ?: getDetail(reason.details, "taskId", 0L)
        val memberId =
            contextMemberId ?: getDetail(reason.details, "memberId", 0L) // Use generic memberId

        return when (reason.code) {
            EligibilityRejectReasonCodeDTO.ALREADY_PARTICIPATING ->
                if (taskId != 0L && memberId != 0L) {
                    AlreadyBeTaskParticipantError(taskId, memberId)
                } else {
                    ForbiddenError(reason.message, reason.details ?: emptyMap()) // Fallback
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
                // Extracting first missing ID might be misleading, return general error or specific
                // user ID
                val missingIds = getDetailNullable<List<Long>>(reason.details, "missingUserIds")
                val firstMissingId = missingIds?.firstOrNull() ?: 0L // Use 0L if list is empty/null
                RealNameInfoRequiredError(firstMissingId) // Error points to one user
            }
            EligibilityRejectReasonCodeDTO.TEAM_MEMBER_RANK_NOT_HIGH_ENOUGH -> {
                val userId = getDetail(reason.details, "userId", 0L)
                val actual = getDetail(reason.details, "actualRank", 0)
                val required = getDetail(reason.details, "requiredRank", 0)
                YourTeamMemberRankIsNotHighEnoughError(userId, actual, required)
            }
            // Default for codes not specifically handled
            else -> ForbiddenError(reason.message, reason.details ?: emptyMap())
        }
    }

    // Helper to safely extract details from the reason map
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

    // Helper to create reason DTO
    private fun createRejectReason(
        code: EligibilityRejectReasonCodeDTO,
        message: String,
        details: Map<String, Any>? = null,
    ): EligibilityRejectReasonInfoDTO {
        return EligibilityRejectReasonInfoDTO(code = code, message = message, details = details)
    }

    // Helper to check rank eligibility
    private fun checkRankEligibility(
        task: Task,
        userId: IdType,
        isUserTaskContext:
            Boolean, // True if checking for a USER task participation, false if checking a TEAM
        // member
        reasons: MutableList<EligibilityRejectReasonInfoDTO>,
    ) {
        val rankConfig = applicationConfig.rankCheckEnforced // Cache config lookup
        val spaceConfig = task.space.enableRank ?: false // Cache space setting
        val taskRankRequirement = task.rank

        if (rankConfig && spaceConfig && taskRankRequirement != null) {
            val requiredRank =
                taskRankRequirement - applicationConfig.rankJump // Calculate required rank once
            if (requiredRank > 0) { // Only check if a positive rank is required
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
                            val username =
                                userService
                                    .getUserDto(userId)
                                    .username // Fetch username for message
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
                                "taskId" to task.id!!, // Add task context
                            ),
                        )
                    )
                }
            }
        }
    }

    // Basic check if user exists and is active (simplified)
    private fun checkUserBasicEligibility(userId: IdType): EligibilityStatusDTO {
        val reasons = mutableListOf<EligibilityRejectReasonInfoDTO>()
        if (!userService.existsUser(userId)) { // Replace with a proper active check if available
            reasons.add(
                createRejectReason(
                    EligibilityRejectReasonCodeDTO.USER_NOT_FOUND,
                    "User not found or inactive.", // More specific message
                    mapOf("userId" to userId),
                )
            )
        }
        // Add checks for banned status etc. if needed
        return EligibilityStatusDTO(eligible = reasons.isEmpty(), reasons = reasons)
    }

    // Checks eligibility for a USER to join a USER task
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
            if (
                !userRealNameService.hasUserIdentity(userId)
            ) { // Use a check method instead of try-catch
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

    // Checks eligibility for a TEAM to join a TEAM task
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

        // Check team existence first
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

        // Fetch members AND their real name status if task requires it
        // Pass queryRealNameStatus = true ONLY if task requires it, for efficiency
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
            // Use the 'allVerified' flag returned by getTeamMembers
            if (allVerified != true) { // Check for explicit true, handles null case
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

    // Gets overall participation eligibility for a user regarding a task
    @Transactional(readOnly = true)
    fun getParticipationEligibility(task: Task, userId: IdType): ParticipationEligibilityDTO {
        return when (task.submitterType) {
            TaskSubmitterType.USER -> {
                val userStatus = checkUserEligibilityForUserTask(task, userId)
                ParticipationEligibilityDTO(user = userStatus, teams = null)
            }
            TaskSubmitterType.TEAM -> {
                // Get teams the user can potentially use (admin/owner roles)
                val potentialTeams = teamService.getTeamsThatUserCanUseToJoinTask(task.id!!, userId)
                val teamsStatus =
                    potentialTeams.map { teamSummary
                        -> // Use TeamSummaryDTO from getTeamsThatUserCanUseToJoinTask
                        val (specificTeamEligibility, memberDetails, allVerified) =
                            checkTeamEligibilityForTeamTask(
                                task,
                                teamSummary.id,
                            ) // Check eligibility for *this* team

                        // Enhance the summary DTO with eligibility details if needed
                        val enhancedTeamSummary =
                            teamSummary.copy(
                                allMembersVerified = allVerified, // Add verification status
                                memberRealNameStatus =
                                    memberDetails
                                        ?.map { // Add member status only if needed/available
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

    // Data class to hold the result of the operation
    data class SnapshotCreationResult(
        val membershipsChecked: Long,
        val membershipsUpdated: Long,
        val snapshotsEntriesCreated: Long,
        val errorsEncountered: Int = 0 // Optional: Count errors
    )

    // Add this new public method to TaskMembershipService
    @Transactional(propagation = Propagation.NEVER) // Use manual transaction control per batch
    fun createMissingTeamSnapshotsForAllTasks(): SnapshotCreationResult {
        val logger = LoggerFactory.getLogger(javaClass) // Get logger instance
        logger.info("Starting process to create missing team snapshots for all tasks...")

        val BATCH_SIZE = 50 // Adjust batch size
        var pageNumber = 0
        var totalProcessed: Long = 0
        var totalMembershipsUpdated: Long = 0
        var totalSnapshotsCreated: Long = 0
        var errorCount: Int = 0
        var hasMore = true

        val startTime = System.currentTimeMillis()

        while (hasMore) {
            val pageable: Pageable = PageRequest.of(pageNumber, BATCH_SIZE)
            logger.info("Processing page {} for missing snapshots...", pageNumber)
            val membershipPage: Page<TaskMembership>

            try {
                // Fetch only team memberships in this batch
                // Ideally, add a query to fetch only those where teamMembersRealNameInfo might be empty,
                // but checking after fetch is reliable if a direct query is hard.
                membershipPage = taskMembershipRepository.findAllByIsTeam(true, pageable) // Assuming this query exists
            } catch (e: Exception) {
                logger.error("Error fetching page {} of team memberships: {}", pageNumber, e.message, e)
                errorCount++
                // Decide whether to continue or stop on fetch error
                break // Stop processing if fetching fails
            }

            val membershipsInBatch = membershipPage.content
            if (membershipsInBatch.isEmpty()) {
                hasMore = false
                if (pageNumber == 0) logger.info("No team memberships found.")
                continue // Go to next iteration check (will exit loop)
            }

            val membershipsToUpdateInBatch = mutableListOf<TaskMembership>()

            for (membership in membershipsInBatch) {
                // Check if snapshot is missing (is null or empty)
                if (membership.teamMembersRealNameInfo.isEmpty()) {
                    logger.debug("Membership ID {} (Team ID {}) needs snapshot.", membership.id, membership.memberId)
                    try {
                        val teamId = membership.memberId
                        if (teamId == null) {
                            logger.warn("Membership ID {} has null memberId (Team ID). Skipping.", membership.id)
                            continue
                        }
                        // Fetch CURRENT team members to build the snapshot
                        val (currentTeamMembers, _) = teamService.getTeamMembers(teamId)

                        if (currentTeamMembers.isNotEmpty()) {
                            val newSnapshot = currentTeamMembers.map { teamMember ->
                                // participantMemberUuid is generated by default value in constructor now
                                TeamMemberRealNameInfo(
                                    memberId = teamMember.user.id,
                                    realNameInfo = DefaultRealNameInfo // Use default for non-real-name tasks
                                )
                            }
                            membership.teamMembersRealNameInfo.clear()
                            membership.teamMembersRealNameInfo.addAll(newSnapshot)
                            membershipsToUpdateInBatch.add(membership)
                            totalSnapshotsCreated += newSnapshot.size
                            logger.debug("Prepared snapshot with {} members for membership ID {}", newSnapshot.size, membership.id)
                        } else {
                            logger.warn("Team ID {} has no members currently. Creating empty snapshot for membership ID {}.", teamId, membership.id)
                            // Add empty list to mark it as processed (optional, depends on desired behavior)
                            membership.teamMembersRealNameInfo.clear()
                            membershipsToUpdateInBatch.add(membership) // Save even with empty snapshot to mark as done
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating snapshot for membership ID {}: {}", membership.id, e.message, e)
                        errorCount++
                        // Continue to next membership on individual error
                    }
                }
            } // End loop through batch content

            // Save updated memberships for this batch in a transaction
            if (membershipsToUpdateInBatch.isNotEmpty()) {
                try {
                    // Call a separate transactional method to save the batch
                    saveMembershipBatchInternal(membershipsToUpdateInBatch)
                    totalMembershipsUpdated += membershipsToUpdateInBatch.size
                    logger.info("Saved batch of {} memberships with new/updated snapshots (Page {}).", membershipsToUpdateInBatch.size, pageNumber)
                } catch (e: Exception) {
                    logger.error("Failed to save batch for page {}: {}", pageNumber, e.message, e)
                    errorCount += membershipsToUpdateInBatch.size // Count all in failed batch as errors
                    // Decide if we should stop or continue on batch save error
                    // continue // Example: Continue to next page despite save error
                    // break // Example: Stop processing on save error
                }
            }

            totalProcessed += membershipsInBatch.size
            hasMore = membershipPage.hasNext()
            pageNumber++

        } // End while loop

        val duration = System.currentTimeMillis() - startTime
        logger.info(
            "Missing snapshot creation process finished in {} ms. Memberships checked: {}, Memberships updated: {}, Snapshot entries created: {}, Errors: {}",
            duration, totalProcessed, totalMembershipsUpdated, totalSnapshotsCreated, errorCount
        )

        return SnapshotCreationResult(totalProcessed, totalMembershipsUpdated, totalSnapshotsCreated, errorCount)
    }

    // Helper method with its own transaction for saving batches
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Run each batch save in a new transaction
    fun saveMembershipBatchInternal(memberships: List<TaskMembership>) {
        taskMembershipRepository.saveAll(memberships)
    }
}
