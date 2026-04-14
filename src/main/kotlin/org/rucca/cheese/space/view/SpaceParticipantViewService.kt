package org.rucca.cheese.space.view

import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.SpaceMyParticipatingOverviewDTO
import org.rucca.cheese.model.SpaceMyParticipationCategoryDTO
import org.rucca.cheese.model.SpaceMyParticipationDTO
import org.rucca.cheese.model.SpaceMyParticipationPublisherDTO
import org.rucca.cheese.model.SpaceMyParticipationsDTO
import org.rucca.cheese.model.TaskCompletionStatusDTO
import org.rucca.cheese.model.TaskSubmitterTypeDTO
import org.rucca.cheese.model.TeamDTO
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskSubmission
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReview
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.toDTO
import org.rucca.cheese.team.TeamService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpaceParticipantViewService(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
    private val teamService: TeamService,
) {
    private enum class ParticipationSortBy {
        JOINED_AT,
        DEADLINE,
        LATEST_SUBMISSION_AT,
        COMPLETION_STATUS,
    }

    private enum class SortOrder {
        ASC,
        DESC,
    }

    private data class ParticipationContext(
        val memberships: List<TaskMembership>,
        val teamById: Map<IdType, TeamDTO>,
        val submissionsByMembershipId: Map<IdType, List<TaskSubmission>>,
        val reviewsBySubmissionId: Map<IdType, TaskSubmissionReview>,
        val currentUserId: IdType,
    )

    fun getOverview(spaceId: IdType, currentUserId: IdType): SpaceMyParticipatingOverviewDTO {
        val rows =
            loadContext(spaceId = spaceId, currentUserId = currentUserId).let { context ->
                context.memberships.map { toParticipationRow(it, context) }
            }

        return SpaceMyParticipatingOverviewDTO(
            spaceId = spaceId,
            participationCount = rows.size,
            approvedParticipationCount =
                rows.count { it.approved.value == ApproveType.APPROVED.name },
            pendingApprovalCount = rows.count { it.approved.value == ApproveType.NONE.name },
            awaitingSubmissionCount =
                rows.count {
                    it.approved.value == ApproveType.APPROVED.name &&
                        it.completionStatus == TaskCompletionStatusDTO.NOT_SUBMITTED
                },
            pendingReviewCount =
                rows.count { it.completionStatus == TaskCompletionStatusDTO.PENDING_REVIEW },
            resubmittableCount =
                rows.count {
                    it.completionStatus == TaskCompletionStatusDTO.REJECTED_RESUBMITTABLE
                },
            successfulCount = rows.count { it.completionStatus == TaskCompletionStatusDTO.SUCCESS },
            failedCount = rows.count { it.completionStatus == TaskCompletionStatusDTO.FAILED },
        )
    }

    fun getParticipations(
        spaceId: IdType,
        currentUserId: IdType,
        approved: String?,
        completionStatus: TaskCompletionStatusDTO?,
        identityType: TaskSubmitterTypeDTO?,
        sortBy: String,
        sortOrder: String,
    ): SpaceMyParticipationsDTO {
        val context = loadContext(spaceId = spaceId, currentUserId = currentUserId)
        val parsedApproved = approved?.takeUnless(String::isBlank)?.let(::parseApproveType)
        val parsedCompletionStatus =
            completionStatus?.let { TaskCompletionStatus.valueOf(it.value) }
        val parsedIdentityType = identityType?.let { TaskSubmitterType.valueOf(it.value) }

        val rows =
            context.memberships
                .map { toParticipationRow(it, context) }
                .filter { row ->
                    (parsedApproved == null || row.approved.value == parsedApproved.name) &&
                        (parsedCompletionStatus == null ||
                            row.completionStatus.value == parsedCompletionStatus.name) &&
                        (parsedIdentityType == null ||
                            row.identityType.value == parsedIdentityType.name)
                }

        val parsedSortBy = parseSortBy(sortBy)
        val parsedSortOrder = parseSortOrder(sortOrder)
        val comparator =
            when (parsedSortBy) {
                ParticipationSortBy.JOINED_AT -> compareBy<SpaceMyParticipationDTO> { it.joinedAt }
                ParticipationSortBy.DEADLINE -> compareBy { it.deadline }
                ParticipationSortBy.LATEST_SUBMISSION_AT -> compareBy { it.latestSubmissionAt }
                ParticipationSortBy.COMPLETION_STATUS -> compareBy { it.completionStatus.value }
            }

        val sortedRows =
            if (parsedSortOrder == SortOrder.ASC) {
                rows.sortedWith(comparator)
            } else {
                rows.sortedWith(comparator.reversed())
            }

        return SpaceMyParticipationsDTO(participations = sortedRows)
    }

    private fun loadContext(spaceId: IdType, currentUserId: IdType): ParticipationContext {
        val teams = teamService.getTeamsOfUser(currentUserId)
        val teamById = teams.associateBy { it.id }
        val memberIds = buildList {
            add(currentUserId)
            addAll(teamById.keys)
        }
        val memberships =
            taskMembershipRepository.findAllByTaskSpaceIdAndMemberIdIn(spaceId, memberIds).filter {
                membership ->
                (!membership.isTeam && membership.memberId == currentUserId) ||
                    (membership.isTeam && teamById.containsKey(membership.memberId))
            }
        val membershipIds = memberships.mapNotNull { it.id }
        val submissionsByMembershipId =
            if (membershipIds.isEmpty()) {
                emptyMap()
            } else {
                taskSubmissionRepository.findAllByMembershipIdIn(membershipIds).groupBy {
                    it.membership!!.id!!
                }
            }
        val submissionIds = submissionsByMembershipId.values.flatten().mapNotNull { it.id }
        val reviewsBySubmissionId =
            if (submissionIds.isEmpty()) {
                emptyMap()
            } else {
                taskSubmissionReviewRepository.findAllBySubmissionIdIn(submissionIds).associateBy {
                    it.submission!!.id!!
                }
            }

        return ParticipationContext(
            memberships = memberships,
            teamById = teamById,
            submissionsByMembershipId = submissionsByMembershipId,
            reviewsBySubmissionId = reviewsBySubmissionId,
            currentUserId = currentUserId,
        )
    }

    private fun toParticipationRow(
        membership: TaskMembership,
        context: ParticipationContext,
    ): SpaceMyParticipationDTO {
        val task = membership.task!!
        val latestSubmission = latestSubmissionOf(membership, context.submissionsByMembershipId)
        val latestReview = latestSubmission?.id?.let(context.reviewsBySubmissionId::get)
        val isTeam = membership.isTeam
        val teamId = membership.memberId

        return SpaceMyParticipationDTO(
            taskId = task.id!!.toLong(),
            taskName = task.name,
            publisher =
                SpaceMyParticipationPublisherDTO(
                    id = task.creator.id!!.toLong(),
                    name = task.creator.username ?: "",
                ),
            category =
                SpaceMyParticipationCategoryDTO(
                    id = task.category.id!!.toLong(),
                    name = task.category.name,
                ),
            participationId = membership.id!!.toLong(),
            identityType =
                if (isTeam) TaskSubmitterType.TEAM.toDTO() else TaskSubmitterType.USER.toDTO(),
            teamName = if (isTeam) context.teamById[teamId]?.name else null,
            approved = membership.approved!!.convert(),
            completionStatus = membership.completionStatus.toDTO(),
            canSubmit = canSubmit(membership, context.currentUserId, context.teamById),
            joinedAt = membership.createdAt.toEpochMilli(),
            deadline = membership.deadline?.toEpochMilli(),
            latestSubmissionAt = latestSubmission?.createdAt?.toEpochMilli(),
            latestReviewAccepted = latestReview?.accepted,
            latestReviewScore = latestReview?.score?.toDouble(),
        )
    }

    private fun canSubmit(
        membership: TaskMembership,
        currentUserId: IdType,
        teamById: Map<IdType, TeamDTO>,
    ): Boolean {
        if (membership.approved != ApproveType.APPROVED) return false
        if (!membership.isTeam) return membership.memberId == currentUserId
        val teamId = membership.memberId ?: return false
        if (!teamById.containsKey(teamId)) return false
        return teamService.isTeamAtLeastAdmin(teamId, currentUserId)
    }

    private fun latestSubmissionOf(
        membership: TaskMembership,
        submissionsByMembershipId: Map<IdType, List<TaskSubmission>>,
    ): TaskSubmission? =
        submissionsByMembershipId[membership.id]
            .orEmpty()
            .maxWithOrNull(compareBy<TaskSubmission>({ it.version }, { it.createdAt }))

    private fun parseApproveType(value: String): ApproveType =
        runCatching { ApproveType.valueOf(value) }
            .getOrElse { throw BadRequestError("Invalid approved: $value") }

    private fun parseSortBy(value: String): ParticipationSortBy =
        when (value) {
            "joinedAt" -> ParticipationSortBy.JOINED_AT
            "deadline" -> ParticipationSortBy.DEADLINE
            "latestSubmissionAt" -> ParticipationSortBy.LATEST_SUBMISSION_AT
            "completionStatus" -> ParticipationSortBy.COMPLETION_STATUS
            else -> throw BadRequestError("Invalid sortBy: $value")
        }

    private fun parseSortOrder(value: String): SortOrder =
        when (value.lowercase()) {
            "asc" -> SortOrder.ASC
            "desc" -> SortOrder.DESC
            else -> throw BadRequestError("Invalid sortOrder: $value")
        }
}
