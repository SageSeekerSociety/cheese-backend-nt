package org.rucca.cheese.task.service

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskParticipantRealNameInfoDTO
import org.rucca.cheese.model.UserIdentityDTO
import org.rucca.cheese.task.*
import org.rucca.cheese.task.error.RealNameInfoRequiredError
import org.rucca.cheese.task.error.TeamSizeNotEnoughError
import org.rucca.cheese.task.error.TeamSizeTooLargeError
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.models.KeyPurpose
import org.rucca.cheese.user.services.EncryptionService
import org.rucca.cheese.user.services.UserRealNameService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskMembershipSnapshotService(
    private val taskMembershipRepository:
        TaskMembershipRepository, // Needed to save updated snapshots
    private val taskRepository: TaskRepository, // Needed for task context (requireRealName)
    private val encryptionService: EncryptionService,
    private val userRealNameService: UserRealNameService,
    private val teamService: TeamService, // Needed to get current team members for snapshots
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    /** Encrypts a UserIdentityDTO using the provided task key ID */
    fun encryptRealNameInfo(identity: UserIdentityDTO, taskKeyId: String): RealNameInfo {
        return RealNameInfo(
            realName = encryptionService.encryptData(identity.realName, taskKeyId),
            studentId = encryptionService.encryptData(identity.studentId, taskKeyId),
            grade = encryptionService.encryptData(identity.grade, taskKeyId),
            major = encryptionService.encryptData(identity.major, taskKeyId),
            className = encryptionService.encryptData(identity.className, taskKeyId),
            encrypted = true,
        )
    }

    /** Decrypts RealNameInfo assuming it's marked encrypted and keyId is valid */
    fun decryptUserRealNameInfo(
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
            .convert() // Convert to DTO
    }

    /** Decrypts RealNameInfo for a team member */
    fun decryptTeamMemberRealNameInfo(
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
            .convert() // Convert to DTO
    }

    /** Retrieves potentially decrypted RealNameInfo from a membership object */
    fun getRealNameInfoFromMembership(membership: TaskMembership): TaskParticipantRealNameInfoDTO? {
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

    /** Retrieves potentially decrypted RealNameInfo for a specific team member from the snapshot */
    fun getRealNameInfoForTeamMemberSnapshot(
        membership: TaskMembership, // Needed for encryptionKeyId
        memberSnapshot: TeamMemberRealNameInfo,
    ): TaskParticipantRealNameInfoDTO {
        val info = memberSnapshot.realNameInfo
        return if (info.encrypted && membership.encryptionKeyId != null) {
            decryptTeamMemberRealNameInfo(info, membership.encryptionKeyId!!)
        } else if (!info.encrypted && info.realName != null) { // Display non-encrypted if available
            info.convert()
        } else {
            DefaultRealNameInfo.convert() // Return default empty DTO
        }
    }

    /** Builds the team snapshot data (list and optional key ID) */
    fun buildTeamSnapshot(task: Task, teamId: IdType): Pair<List<TeamMemberRealNameInfo>, String?> {
        val (currentTeamMembers, _) =
            teamService.getTeamMembers(teamId = teamId, queryRealNameStatus = task.requireRealName)

        if (currentTeamMembers.isEmpty() && task.minTeamSize != null && task.minTeamSize!! > 0) {
            throw TeamSizeNotEnoughError(0, task.minTeamSize!!)
        }

        val snapshotList = mutableListOf<TeamMemberRealNameInfo>()
        var encryptionKeyIdToSave: String? = null
        var actualEncryptionHappened = false

        if (task.requireRealName) {
            val taskKey = encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, task.id!!)
            encryptionKeyIdToSave = taskKey.id
        }

        for (teamMember in currentTeamMembers) {
            val currentMemberUserId = teamMember.user.id
            val memberRealNameInfoForSnapshot: RealNameInfo

            if (task.requireRealName) {
                val userIdentity: UserIdentityDTO =
                    try {
                        userRealNameService.getUserIdentity(currentMemberUserId)
                    } catch (e: NotFoundError) {
                        logger.error(
                            "CRITICAL: Real name identity for user {} not found during snapshot creation for task {}.",
                            currentMemberUserId,
                            task.id,
                        )
                        throw RealNameInfoRequiredError(currentMemberUserId)
                    }

                if (encryptionKeyIdToSave == null) {
                    throw IllegalStateException(
                        "Encryption key ID is null despite task requiring real name."
                    )
                }

                memberRealNameInfoForSnapshot =
                    encryptRealNameInfo(userIdentity, encryptionKeyIdToSave)
                actualEncryptionHappened = true
            } else {
                memberRealNameInfoForSnapshot = DefaultRealNameInfo
            }

            snapshotList.add(
                TeamMemberRealNameInfo(
                    memberId = currentMemberUserId,
                    realNameInfo = memberRealNameInfoForSnapshot,
                    // participantMemberUuid has default value in constructor
                )
            )
        }

        val snapshotSize = snapshotList.size
        task.minTeamSize?.let { min ->
            if (snapshotSize < min) throw TeamSizeNotEnoughError(snapshotSize, min)
        }
        task.maxTeamSize?.let { max ->
            if (snapshotSize > max) throw TeamSizeTooLargeError(snapshotSize, max)
        }

        return Pair(snapshotList, if (actualEncryptionHappened) encryptionKeyIdToSave else null)
    }

    /** Fixes missing real name information snapshots in TaskMemberships for a given task */
    @Transactional(propagation = Propagation.REQUIRED)
    fun fixRealNameInfoForTask(taskId: IdType): Int {
        logger.info("Starting real name info fix process for task ID: {}", taskId)
        val task = getTask(taskId)

        if (!task.requireRealName) {
            logger.warn("Task ID: {} does not require real name. Fix function skipped.", taskId)
            return 0
        }

        val taskEncryptionKey = encryptionService.getOrCreateKey(KeyPurpose.TASK_REAL_NAME, taskId)
        val taskKeyId = taskEncryptionKey.id
        logger.debug("Using encryption key ID: {} for task ID: {}", taskKeyId, taskId)

        val memberships = taskMembershipRepository.findAllByTaskId(taskId)
        val updatedMemberships = mutableListOf<TaskMembership>()
        var updatedCount = 0

        logger.info("Found {} memberships for task ID: {}", memberships.size, taskId)

        for (membership in memberships) {
            var needsUpdate = false
            val memberId = membership.memberId ?: continue

            try {
                if (!membership.isTeam) {
                    val userId = memberId
                    if (membership.realNameInfo == null || !membership.realNameInfo!!.encrypted) {
                        logger.debug(
                            "Processing individual membership ID: {} (Needs Fix)",
                            membership.id,
                        )
                        val userIdentity =
                            try {
                                userRealNameService.getUserIdentity(userId)
                            } catch (e: NotFoundError) {
                                logger.error(
                                    "User real name identity not found for user ID: {}. Skipping fix.",
                                    userId,
                                )
                                null
                            }

                        if (userIdentity != null) {
                            val encryptedInfo = encryptRealNameInfo(userIdentity, taskKeyId)
                            membership.realNameInfo = encryptedInfo
                            membership.encryptionKeyId = taskKeyId
                            needsUpdate = true
                            logger.info(
                                "Prepared fix for individual membership ID: {}",
                                membership.id,
                            )
                        }
                    } else {
                        logger.debug(
                            "Individual membership ID: {} already fixed. Skipping.",
                            membership.id,
                        )
                    }
                } else { // Is Team
                    val teamId = memberId
                    val requiresTeamFix =
                        membership.teamMembersRealNameInfo.isEmpty() ||
                            membership.teamMembersRealNameInfo.any { !it.realNameInfo.encrypted }

                    if (requiresTeamFix) {
                        logger.debug("Processing team membership ID: {} (Needs Fix)", membership.id)
                        val (teamMembers, _) = teamService.getTeamMembers(teamId)
                        if (teamMembers.isEmpty()) {
                            logger.warn("Team ID: {} has no members. Cannot fix.", teamId)
                            continue
                        }

                        val newTeamMembersRealNameInfo = mutableListOf<TeamMemberRealNameInfo>()
                        var teamFixPossible = true

                        for (teamMember in teamMembers) {
                            val teamMemberUserId = teamMember.user.id
                            val memberIdentity =
                                try {
                                    userRealNameService.getUserIdentity(teamMemberUserId)
                                } catch (e: NotFoundError) {
                                    logger.error(
                                        "User real name identity not found for team member ID: {}. Skipping fix for team.",
                                        teamMemberUserId,
                                    )
                                    teamFixPossible = false
                                    break
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
                            membership.encryptionKeyId = taskKeyId
                            needsUpdate = true
                            logger.info("Prepared fix for team membership ID: {}", membership.id)
                        } else if (!teamFixPossible) {
                            logger.warn(
                                "Skipped fix for team membership ID: {} due to missing member info.",
                                membership.id,
                            )
                        }
                    } else {
                        logger.debug(
                            "Team membership ID: {} already fixed. Skipping.",
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
                    "Unexpected error processing membership ID: {}. Skipping.",
                    membership.id,
                    e,
                )
            }
        }

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

    data class SnapshotCreationResult(
        val membershipsChecked: Long,
        val membershipsUpdated: Long,
        val snapshotsEntriesCreated: Long,
        val errorsEncountered: Int = 0,
    )

    @Transactional(propagation = Propagation.NEVER) // Uses manual transaction control per batch
    fun createMissingTeamSnapshotsForAllTasks(): SnapshotCreationResult {
        logger.info("Starting process to create missing team snapshots...")
        val BATCH_SIZE = 50
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
                membershipPage = taskMembershipRepository.findAllByIsTeam(true, pageable)
            } catch (e: Exception) {
                logger.error(
                    "Error fetching page {} of team memberships: {}",
                    pageNumber,
                    e.message,
                    e,
                )
                errorCount++
                break
            }

            val membershipsInBatch = membershipPage.content
            if (membershipsInBatch.isEmpty()) {
                hasMore = false
                if (pageNumber == 0) logger.info("No team memberships found.")
                continue
            }

            val membershipsToUpdateInBatch = mutableListOf<TaskMembership>()

            for (membership in membershipsInBatch) {
                if (membership.teamMembersRealNameInfo.isEmpty()) {
                    logger.debug("Membership ID {} needs snapshot.", membership.id)
                    try {
                        val teamId = membership.memberId
                        if (teamId == null) {
                            logger.warn(
                                "Membership ID {} has null memberId. Skipping.",
                                membership.id,
                            )
                            continue
                        }
                        val (currentTeamMembers, _) = teamService.getTeamMembers(teamId)

                        if (currentTeamMembers.isNotEmpty()) {
                            val newSnapshot =
                                currentTeamMembers.map { teamMember ->
                                    TeamMemberRealNameInfo(
                                        memberId = teamMember.user.id,
                                        realNameInfo = DefaultRealNameInfo,
                                    )
                                }
                            membership.teamMembersRealNameInfo.clear()
                            membership.teamMembersRealNameInfo.addAll(newSnapshot)
                            membershipsToUpdateInBatch.add(membership)
                            totalSnapshotsCreated += newSnapshot.size
                            logger.debug("Prepared snapshot for membership ID {}", membership.id)
                        } else {
                            logger.warn(
                                "Team ID {} has no members. Creating empty snapshot for membership ID {}.",
                                teamId,
                                membership.id,
                            )
                            membership.teamMembersRealNameInfo.clear()
                            membershipsToUpdateInBatch.add(membership)
                        }
                    } catch (e: Exception) {
                        logger.error(
                            "Error creating snapshot for membership ID {}: {}",
                            membership.id,
                            e.message,
                            e,
                        )
                        errorCount++
                    }
                }
            } // End loop batch content

            if (membershipsToUpdateInBatch.isNotEmpty()) {
                try {
                    saveMembershipBatchInternal(
                        membershipsToUpdateInBatch
                    ) // Call transactional helper
                    totalMembershipsUpdated += membershipsToUpdateInBatch.size
                    logger.info("Saved batch for page {}.", pageNumber)
                } catch (e: Exception) {
                    logger.error("Failed to save batch for page {}: {}", pageNumber, e.message, e)
                    errorCount += membershipsToUpdateInBatch.size
                }
            }

            totalProcessed += membershipsInBatch.size
            hasMore = membershipPage.hasNext()
            pageNumber++
        } // End while loop

        val duration = System.currentTimeMillis() - startTime
        logger.info(
            "Snapshot creation finished in {} ms. Checked: {}, Updated: {}, Entries Created: {}, Errors: {}",
            duration,
            totalProcessed,
            totalMembershipsUpdated,
            totalSnapshotsCreated,
            errorCount,
        )

        return SnapshotCreationResult(
            totalProcessed,
            totalMembershipsUpdated,
            totalSnapshotsCreated,
            errorCount,
        )
    }

    /** Helper method with its own transaction for saving batches */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveMembershipBatchInternal(memberships: List<TaskMembership>) {
        taskMembershipRepository.saveAll(memberships)
    }
}
