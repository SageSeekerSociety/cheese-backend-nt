package org.rucca.cheese.space.view

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.SpaceMyPublishedTaskCategoryDTO
import org.rucca.cheese.model.SpaceMyPublishedTaskDTO
import org.rucca.cheese.model.SpaceMyPublishedTasksDTO
import org.rucca.cheese.model.SpaceMyPublishingOverviewDTO
import org.rucca.cheese.task.Task
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembership
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.TaskRepository
import org.rucca.cheese.task.TaskSubmission
import org.rucca.cheese.task.TaskSubmissionRepository
import org.rucca.cheese.task.TaskSubmissionReview
import org.rucca.cheese.task.TaskSubmissionReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpacePublisherViewService(
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
) {
    private data class PublisherTaskContext(
        val tasks: List<Task>,
        val membershipsByTaskId: Map<IdType, List<TaskMembership>>,
        val submissionsByMembershipId: Map<IdType, List<TaskSubmission>>,
        val reviewsBySubmissionId: Map<IdType, TaskSubmissionReview>,
    )

    private data class TaskSummary(
        val task: Task,
        val participantCount: Int,
        val approvedParticipantCount: Int,
        val pendingParticipantApprovalCount: Int,
        val submittedParticipantCount: Int,
        val pendingReviewCount: Int,
        val successfulParticipantCount: Int,
        val failedParticipantCount: Int,
        val submissionConversionRate: Double,
        val successRate: Double,
        val latestSubmissionAt: Long?,
    )

    fun getOverview(spaceId: IdType, currentUserId: IdType): SpaceMyPublishingOverviewDTO {
        val context =
            loadContext(
                spaceId = spaceId,
                currentUserId = currentUserId,
                from = null,
                to = null,
                categoryId = null,
                approved = null,
            )
        val summaries = context.tasks.map { toTaskSummary(it, context) }

        return SpaceMyPublishingOverviewDTO(
            spaceId = spaceId,
            taskCount = context.tasks.size,
            approvedTaskCount = context.tasks.count { it.approved == ApproveType.APPROVED },
            pendingTaskApprovalCount = context.tasks.count { it.approved == ApproveType.NONE },
            disapprovedTaskCount = context.tasks.count { it.approved == ApproveType.DISAPPROVED },
            participantCount = summaries.sumOf { it.participantCount },
            approvedParticipantCount = summaries.sumOf { it.approvedParticipantCount },
            pendingParticipantApprovalCount =
                summaries.sumOf { it.pendingParticipantApprovalCount },
            submittedParticipantCount = summaries.sumOf { it.submittedParticipantCount },
            pendingReviewCount = summaries.sumOf { it.pendingReviewCount },
            successfulParticipantCount = summaries.sumOf { it.successfulParticipantCount },
        )
    }

    fun getPublishedTasks(
        spaceId: IdType,
        currentUserId: IdType,
        from: Long?,
        to: Long?,
        categoryId: Long?,
        approved: String?,
        hasPendingParticipantApproval: Boolean?,
        hasPendingReview: Boolean?,
        sortBy: String,
        sortOrder: String,
    ): SpaceMyPublishedTasksDTO {
        val context =
            loadContext(
                spaceId = spaceId,
                currentUserId = currentUserId,
                from = from,
                to = to,
                categoryId = categoryId,
                approved = approved,
            )

        val summaries =
            context.tasks
                .map { toTaskSummary(it, context) }
                .filter { summary ->
                    (hasPendingParticipantApproval == null ||
                        (summary.pendingParticipantApprovalCount > 0) ==
                            hasPendingParticipantApproval) &&
                        (hasPendingReview == null ||
                            (summary.pendingReviewCount > 0) == hasPendingReview)
                }

        val comparator =
            when (sortBy) {
                "participantCount" -> compareBy<TaskSummary> { it.participantCount }
                "pendingReviewCount" -> compareBy<TaskSummary> { it.pendingReviewCount }
                "successRate" -> compareBy<TaskSummary> { it.successRate }
                else -> compareBy { it.task.createdAt }
            }

        val sortedSummaries =
            if (sortOrder.equals("asc", ignoreCase = true)) {
                summaries.sortedWith(comparator)
            } else {
                summaries.sortedWith(comparator.reversed())
            }

        return SpaceMyPublishedTasksDTO(
            tasks =
                sortedSummaries.map { summary ->
                    SpaceMyPublishedTaskDTO(
                        taskId = summary.task.id!!.toLong(),
                        taskName = summary.task.name,
                        category =
                            SpaceMyPublishedTaskCategoryDTO(
                                id = summary.task.category.id!!.toLong(),
                                name = summary.task.category.name,
                            ),
                        approved = summary.task.approved.convert(),
                        createdAt = summary.task.createdAt.toEpochMilli(),
                        participantCount = summary.participantCount,
                        approvedParticipantCount = summary.approvedParticipantCount,
                        pendingParticipantApprovalCount = summary.pendingParticipantApprovalCount,
                        submittedParticipantCount = summary.submittedParticipantCount,
                        pendingReviewCount = summary.pendingReviewCount,
                        successfulParticipantCount = summary.successfulParticipantCount,
                        failedParticipantCount = summary.failedParticipantCount,
                        submissionConversionRate = summary.submissionConversionRate,
                        successRate = summary.successRate,
                        deadline = summary.task.deadline?.toEpochMilli(),
                        latestSubmissionAt = summary.latestSubmissionAt,
                    )
                }
        )
    }

    private fun loadContext(
        spaceId: IdType,
        currentUserId: IdType,
        from: Long?,
        to: Long?,
        categoryId: IdType?,
        approved: String?,
    ): PublisherTaskContext {
        val tasks =
            taskRepository.findAnalyticsTasks(
                spaceId = spaceId,
                from = from?.let(::toUtcLocalDateTime),
                to = to?.let(::toUtcLocalDateTime),
                categoryId = categoryId,
                publisherId = currentUserId,
                approved = approved?.takeUnless(String::isBlank)?.let(::parseApproveType),
            )
        val membershipsByTaskId = loadMembershipsByTaskId(tasks)
        val submissionsByMembershipId =
            loadSubmissionsByMembershipId(membershipsByTaskId.values.flatten())
        val reviewsBySubmissionId =
            loadReviewsBySubmissionId(submissionsByMembershipId.values.flatten())

        return PublisherTaskContext(
            tasks = tasks,
            membershipsByTaskId = membershipsByTaskId,
            submissionsByMembershipId = submissionsByMembershipId,
            reviewsBySubmissionId = reviewsBySubmissionId,
        )
    }

    private fun toTaskSummary(task: Task, context: PublisherTaskContext): TaskSummary {
        val memberships = context.membershipsByTaskId[task.id].orEmpty()
        val participantCount = memberships.size
        val approvedParticipantCount = memberships.count { it.approved == ApproveType.APPROVED }
        val pendingParticipantApprovalCount = memberships.count { it.approved == ApproveType.NONE }
        val submittedParticipantCount =
            memberships.count { context.submissionsByMembershipId[it.id].orEmpty().isNotEmpty() }
        val pendingReviewCount =
            memberships.count {
                latestSubmissionOf(it, context.submissionsByMembershipId)?.let { latest ->
                    context.reviewsBySubmissionId[latest.id] == null
                } == true
            }
        val successfulParticipantCount =
            memberships.count { it.completionStatus == TaskCompletionStatus.SUCCESS }
        val failedParticipantCount =
            memberships.count { it.completionStatus == TaskCompletionStatus.FAILED }
        val latestSubmissionAt =
            memberships
                .mapNotNull { latestSubmissionOf(it, context.submissionsByMembershipId)?.createdAt }
                .maxOrNull()
                ?.toEpochMilli()

        return TaskSummary(
            task = task,
            participantCount = participantCount,
            approvedParticipantCount = approvedParticipantCount,
            pendingParticipantApprovalCount = pendingParticipantApprovalCount,
            submittedParticipantCount = submittedParticipantCount,
            pendingReviewCount = pendingReviewCount,
            successfulParticipantCount = successfulParticipantCount,
            failedParticipantCount = failedParticipantCount,
            submissionConversionRate =
                safeRate(submittedParticipantCount, approvedParticipantCount),
            successRate = safeRate(successfulParticipantCount, participantCount),
            latestSubmissionAt = latestSubmissionAt,
        )
    }

    private fun loadMembershipsByTaskId(tasks: List<Task>): Map<IdType, List<TaskMembership>> {
        val taskIds = tasks.mapNotNull { it.id }
        if (taskIds.isEmpty()) return emptyMap()
        return taskMembershipRepository.findAllByTaskIdIn(taskIds).groupBy { it.task!!.id!! }
    }

    private fun loadSubmissionsByMembershipId(
        memberships: List<TaskMembership>
    ): Map<IdType, List<TaskSubmission>> {
        val membershipIds = memberships.mapNotNull { it.id }
        if (membershipIds.isEmpty()) return emptyMap()
        return taskSubmissionRepository.findAllByMembershipIdIn(membershipIds).groupBy {
            it.membership!!.id!!
        }
    }

    private fun loadReviewsBySubmissionId(
        submissions: List<TaskSubmission>
    ): Map<IdType, TaskSubmissionReview> {
        val submissionIds = submissions.mapNotNull { it.id }
        if (submissionIds.isEmpty()) return emptyMap()
        return taskSubmissionReviewRepository.findAllBySubmissionIdIn(submissionIds).associateBy {
            it.submission!!.id!!
        }
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

    private fun toUtcLocalDateTime(epochMilli: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC)

    private fun safeRate(numerator: Int, denominator: Int): Double =
        if (denominator == 0) 0.0 else numerator.toDouble() / denominator.toDouble()
}
