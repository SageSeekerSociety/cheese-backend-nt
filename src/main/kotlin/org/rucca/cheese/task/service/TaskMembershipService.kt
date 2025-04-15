package org.rucca.cheese.task.service

import java.time.LocalDateTime
import org.rucca.cheese.common.error.*
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.*
import org.rucca.cheese.task.*
import org.rucca.cheese.task.error.EmailOrPhoneRequiredError
import org.rucca.cheese.task.error.NotTaskParticipantYetError
import org.rucca.cheese.task.event.TaskMembershipStatusUpdateEvent
import org.rucca.cheese.user.models.KeyPurpose
import org.rucca.cheese.user.services.EncryptionService
import org.rucca.cheese.user.services.UserRealNameService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskMembershipService(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskRepository: TaskRepository,
    private val entityPatcher: EntityPatcher,
    private val eligibilityService: TaskMembershipEligibilityService,
    private val snapshotService: TaskMembershipSnapshotService,
    private val viewService: TaskMembershipViewService,
    private val eventPublisher: ApplicationEventPublisher,
    private val userRealNameService: UserRealNameService,
    private val encryptionService: EncryptionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // --- Core Entity Retrieval ---
    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    private fun getTaskMembership(participantId: IdType): TaskMembership {
        return taskMembershipRepository.findById(participantId).orElseThrow {
            NotFoundError("task participant", participantId)
        }
    }

    private fun getTaskMembershipInternal(
        participantId: IdType? = null,
        taskId: IdType? = null,
        memberId: IdType? = null,
    ): TaskMembership {
        return when {
            participantId != null -> getTaskMembership(participantId)
            taskId != null && memberId != null -> {
                taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                    NotTaskParticipantYetError(taskId, memberId)
                }
            }
            else ->
                throw IllegalArgumentException(
                    "Either participantId or both taskId and memberId must be provided."
                )
        }
    }

    // --- Get Simple IDs (Can stay here or move to ViewService) ---
    fun getTaskParticipantMemberId(participantId: IdType): IdType {
        // Okay to keep simple lookups here
        return getTaskMembership(participantId).memberId!!
    }

    @Transactional(readOnly = true)
    fun getUserParticipantId(taskId: IdType, userId: IdType): IdType? {
        return taskMembershipRepository
            .findByTaskIdAndMemberId(taskId, userId)
            .filter { !it.isTeam }
            .map { it.id!! }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getTeamParticipantId(taskId: IdType, teamId: IdType): IdType? {
        return taskMembershipRepository
            .findByTaskIdAndMemberId(taskId, teamId)
            .filter { it.isTeam }
            .map { it.id!! }
            .orElse(null)
    }

    // --- Core CRUD Operations (Orchestration) ---

    @Transactional
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
    ): TaskMembershipDTO { // Returns DTO via ViewService
        logger.info("Attempting to add participant {} to task {}", memberId, taskId)
        if (email.isNullOrBlank() && phone.isNullOrBlank()) {
            throw EmailOrPhoneRequiredError(taskId, memberId)
        }

        val task = getTask(taskId)
        val isTeam = task.submitterType == TaskSubmitterType.TEAM

        // 1. Check Eligibility (Delegate)
        val eligibility: EligibilityStatusDTO
        var teamMembersForSnapshot: List<TeamMemberDTO>? = null // Store from eligibility check

        when (task.submitterType) {
            TaskSubmitterType.USER -> {
                eligibility = eligibilityService.checkUserEligibilityForUserTask(task, memberId)
            }
            TaskSubmitterType.TEAM -> {
                // Eligibility check now returns the members list to avoid re-fetching
                val (teamEligibility, fetchedMembers, _) =
                    eligibilityService.checkTeamEligibilityForTeamTask(task, memberId)
                eligibility = teamEligibility
                teamMembersForSnapshot = fetchedMembers // Capture for snapshot creation
            }
        }

        if (!eligibility.eligible) {
            throw eligibilityService.mapReasonToError(
                eligibility.reasons!!.first(),
                taskId,
                memberId,
            )
        }

        // 2. Prepare Snapshot Data (Delegate)
        var individualRealNameSnapshot: RealNameInfo? = null
        var teamMemberSnapshotList = mutableListOf<TeamMemberRealNameInfo>()
        var encryptionKeyIdToSave: String? = null

        if (isTeam) {
            // Delegate snapshot building
            try {
                val (snapshotList, keyId) =
                    snapshotService.buildTeamSnapshot(
                        task,
                        memberId,
                    ) // Use the captured member list if possible? `buildTeamSnapshot` currently
                // fetches again. Consider optimization.
                teamMemberSnapshotList = snapshotList.toMutableList()
                encryptionKeyIdToSave = keyId
            } catch (e: Exception) {
                logger.error(
                    "Failed to build team snapshot during add participant: {}",
                    e.message,
                    e,
                )
                // Convert specific errors if needed, or rethrow a generic error
                throw InternalServerError(
                    "Failed to create team snapshot during participation: ${e.message}"
                )
            }
        } else { // Is User
            if (task.requireRealName) {
                try {
                    val userIdentity = userRealNameService.getUserIdentity(memberId)

                    val taskKey =
                        encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)
                    individualRealNameSnapshot =
                        snapshotService.encryptRealNameInfo(userIdentity, taskKey.id)
                    encryptionKeyIdToSave = taskKey.id
                } catch (e: NotFoundError) {
                    throw eligibilityService.mapReasonToError(
                        EligibilityRejectReasonInfoDTO(
                            EligibilityRejectReasonCodeDTO.USER_MISSING_REAL_NAME,
                            "",
                        ),
                        taskId,
                        memberId,
                    ) // Reuse error mapping
                } catch (e: Exception) {
                    logger.error("Failed to prepare individual real name info: {}", e.message, e)
                    throw InternalServerError(
                        "Failed to prepare real name information: ${e.message}"
                    )
                }
            } else {
                individualRealNameSnapshot = null // Or DefaultRealNameInfo
            }
        }

        // 3. Create and Save Entity
        val membership =
            TaskMembership(
                task = task,
                memberId = memberId,
                deadline = deadline,
                approved = approved,
                isTeam = isTeam,
                realNameInfo = individualRealNameSnapshot,
                teamMembersRealNameInfo = teamMemberSnapshotList,
                email = email ?: "",
                phone = phone ?: "",
                applyReason = applyReason ?: "",
                personalAdvantage = personalAdvantage ?: "",
                remark = remark ?: "",
                encryptionKeyId = encryptionKeyIdToSave, // Save key ID only if encryption happened
            )
        val savedMembership = taskMembershipRepository.save(membership)
        logger.info("Saved TaskMembership ID: {}", savedMembership.id)

        // 4. Post-Save Actions (Delegate)
        eligibilityService.autoRejectParticipantAfterReachesLimit(taskId)

        eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, savedMembership.id!!))
        logger.debug(
            "Published status update event after creating membership {}",
            savedMembership.id!!,
        )

        // 5. Return DTO (Delegate)
        return viewService.getTaskMembershipDTO(taskId, savedMembership.memberId!!)
    }

    @Transactional
    fun updateTaskMembership(
        taskId: IdType,
        memberId: IdType,
        patchDto: PatchTaskMembershipRequestDTO,
    ): TaskMembershipDTO {
        val participant = getTaskMembershipInternal(taskId = taskId, memberId = memberId)
        val updatedParticipant = performMembershipUpdateInternal(participant, patchDto)
        // Delegate DTO retrieval
        return viewService.getTaskMembershipDTO(
            updatedParticipant.task!!.id!!,
            updatedParticipant.memberId!!,
        )
    }

    @Transactional
    fun updateTaskMembership(
        participantId: IdType,
        patchDto: PatchTaskMembershipRequestDTO,
    ): TaskMembershipDTO {
        val participant = getTaskMembershipInternal(participantId = participantId)
        val updatedParticipant = performMembershipUpdateInternal(participant, patchDto)
        // Delegate DTO retrieval
        return viewService.getTaskMembershipDTO(
            updatedParticipant.task!!.id!!,
            updatedParticipant.memberId!!,
        )
    }

    /** Internal update logic, now orchestrates calls */
    private fun performMembershipUpdateInternal(
        participant: TaskMembership,
        patchDto: PatchTaskMembershipRequestDTO,
    ): TaskMembership { // Return entity for potential chaining if needed
        val task =
            participant.task
                ?: throw IllegalStateException("Task null for participant ${participant.id}")
        val taskId = task.id!!
        val memberId = participant.memberId!!
        val isTeam = participant.isTeam
        val previousApprovedStatus = participant.approved

        val isApproving =
            patchDto.approved == ApproveTypeDTO.APPROVED &&
                previousApprovedStatus != ApproveType.APPROVED
        val newApproveStatus: ApproveType? = patchDto.approved?.convert()

        // 1. Pre-Approval Checks (Delegate)
        if (isApproving) {
            eligibilityService.ensureTaskParticipantNotReachedLimit(taskId)
            eligibilityService.performPreApprovalChecks(task, memberId, isTeam)
        }

        // Check deadline logic (can stay here as it's simple validation)
        val newDeadline: LocalDateTime? = patchDto.deadline?.toLocalDateTime()
        if (
            newDeadline != null &&
                participant.approved != ApproveType.APPROVED &&
                newApproveStatus != ApproveType.APPROVED
        ) {
            throw ForbiddenError(
                "Cannot set deadline for non-approved membership",
                mapOf("taskId" to taskId, "participantId" to participant.id!!),
            )
        }

        // 2. Prepare Snapshot Update (Delegate, only if approving a team)
        var snapshotData: Pair<List<TeamMemberRealNameInfo>, String?>? = null
        if (isApproving && isTeam) {
            logger.info(
                "Preparing team snapshot update for membership ID {} during approval.",
                participant.id,
            )
            try {
                // Delegate snapshot building
                snapshotData = snapshotService.buildTeamSnapshot(task, memberId)
            } catch (e: Exception) {
                logger.error("Failed to build snapshot during patch: {}", participant.id, e)
                throw BadRequestError(
                    "Failed to prepare team snapshot during approval: ${e.message}"
                )
            }
        }

        // 3. Patch Entity Fields
        entityPatcher.patch(participant, patchDto) {
            handle(PatchTaskMembershipRequestDTO::deadline) { entity, value ->
                entity.deadline = value.toLocalDateTime()
            }
            handle(PatchTaskMembershipRequestDTO::approved) { entity, value ->
                entity.approved = value.convert()
            }
            // Add other simple field mappings here if needed
        }

        // 4. Apply Snapshot Data (if prepared)
        if (snapshotData != null) {
            participant.teamMembersRealNameInfo.clear()
            participant.teamMembersRealNameInfo.addAll(snapshotData.first)
            participant.encryptionKeyId = snapshotData.second
            logger.info("Applying updated snapshot for membership ID {}", participant.id)
        }

        // 5. Save Updated Entity
        val savedParticipant = taskMembershipRepository.save(participant)

        // 6. Publish Status Update Event *AFTER* successful save within the transaction
        eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, savedParticipant.id!!))
        logger.debug(
            "Published status update event after updating membership {}",
            savedParticipant.id!!,
        )

        // 7. Post-Save Actions (Delegate)
        if (
            savedParticipant.approved == ApproveType.APPROVED &&
                previousApprovedStatus != ApproveType.APPROVED
        ) {
            eligibilityService.autoRejectParticipantAfterReachesLimit(taskId)
        }

        return savedParticipant
    }

    @Transactional
    fun removeTaskParticipant(taskId: IdType, participantId: IdType) {
        val participant = getTaskMembership(participantId) // Use internal getter
        if (participant.task?.id != taskId) {
            throw BadRequestError("Task ID mismatch for participant ID: $participantId")
        }
        // Soft delete logic remains simple, can stay here
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
        logger.info("Soft deleted TaskMembership ID: {}", participantId)
    }

    @Transactional
    fun removeTaskParticipantByMemberId(taskId: IdType, memberId: IdType) {
        val participant =
            getTaskMembershipInternal(taskId = taskId, memberId = memberId) // Use internal getter
        // Soft delete logic remains simple, can stay here
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
        logger.info("Soft deleted TaskMembership for task {} and member {}", taskId, memberId)
    }

    // --- Delegating Complex View/Query Methods ---

    @Transactional(readOnly = true)
    fun getTaskMembershipDTO(taskId: IdType, memberId: IdType): TaskMembershipDTO {
        // Delegate to ViewService
        return viewService.getTaskMembershipDTO(taskId, memberId)
    }

    @Transactional(readOnly = true)
    fun getTaskMembershipDTOs(taskId: IdType, approveType: ApproveType?): List<TaskMembershipDTO> {
        // Delegate to ViewService
        return viewService.getTaskMembershipDTOs(taskId, approveType)
    }

    @Transactional(readOnly = true)
    fun getUserParticipationInfo(taskId: IdType, userId: IdType): TaskParticipationInfoDTO {
        // Delegate to ViewService
        return viewService.getUserParticipationInfo(taskId, userId)
    }

    @Transactional(readOnly = true)
    fun getSubmittability(taskId: IdType, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        // Delegate to ViewService
        val task = getTask(taskId) // Fetch task context needed by view service method
        return viewService.getSubmittability(task, userId)
    }

    @Transactional(readOnly = true)
    fun getJoined(taskId: IdType, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        // Delegate to ViewService
        val task = getTask(taskId) // Fetch task context needed by view service method
        return viewService.getJoined(task, userId)
    }

    @Transactional(readOnly = true)
    fun getUserDeadline(taskId: IdType, userId: IdType): Long? {
        // Delegate to ViewService
        return viewService.getUserDeadline(taskId, userId)
    }

    @Transactional(readOnly = true)
    fun isTaskParticipant(taskId: IdType, userId: IdType): Boolean {
        // Delegate to ViewService
        return viewService.isTaskParticipant(taskId, userId)
    }

    @Transactional(readOnly = true)
    fun getParticipationEligibility(taskId: IdType, userId: IdType): ParticipationEligibilityDTO {
        // Delegate to EligibilityService
        val task = getTask(taskId) // Fetch task context needed by eligibility service method
        return eligibilityService.getParticipationEligibility(task, userId)
    }

    fun fixRealNameInfoForTask(taskId: IdType): Int {
        return snapshotService.fixRealNameInfoForTask(taskId)
    }

    fun createMissingTeamSnapshotsForAllTasks():
        TaskMembershipSnapshotService.SnapshotCreationResult {
        return snapshotService.createMissingTeamSnapshotsForAllTasks()
    }
}
