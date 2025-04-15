package org.rucca.cheese.task.service

import java.time.LocalDateTime
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.task.* // Import necessary task entities and enums
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TaskMembershipStatusService(
    // Inject necessary repositories
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val taskSubmissionReviewRepository: TaskSubmissionReviewRepository,
) {

    private val log = LoggerFactory.getLogger(TaskMembershipStatusService::class.java)

    /**
     * Calculates and updates the completion status for a given TaskMembership. Uses REQUIRES_NEW
     * propagation to ensure update happens in its own transaction, useful when called from
     * listeners after commit or from scheduler.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateCompletionStatus(membershipId: IdType) {
        log.debug("Updating completion status for membership ID: {}", membershipId)
        val membershipOpt = taskMembershipRepository.findById(membershipId)
        if (membershipOpt.isEmpty) {
            log.warn("TaskMembership with ID {} not found for status update.", membershipId)
            return
        }
        val membership = membershipOpt.get()
        // Eagerly fetch task if needed, or rely on lazy loading within transaction
        val task =
            membership.task
                ?: run {
                    log.error(
                        "Task is null for TaskMembership ID {}. Cannot update status.",
                        membershipId,
                    )
                    // Consider fetching task explicitly if necessary: taskRepository.findById(...)
                    return // Or throw exception
                }

        // Fetch submissions and reviews efficiently
        val submissions =
            taskSubmissionRepository.findAllByMembershipId(membershipId).sortedByDescending {
                it.createdAt
            } // Latest first
        val submissionIds = submissions.mapNotNull { it.id }
        // Fetch reviews for these submissions (consider findBySubmissionIdIn if available)
        val reviews =
            if (submissionIds.isNotEmpty()) {
                submissionIds
                    .mapNotNull {
                        taskSubmissionReviewRepository.findBySubmissionId(it).orElse(null)
                    }
                    .associateBy { it.submission?.id } // Map: SubmissionID -> Review
            } else {
                emptyMap()
            }

        val newStatus = calculateStatus(membership, task, submissions, reviews)

        if (membership.completionStatus != newStatus) {
            log.info(
                "Updating completion status for membership ID {} from {} to {}",
                membershipId,
                membership.completionStatus,
                newStatus,
            )
            membership.completionStatus = newStatus
            taskMembershipRepository.save(membership) // Save within this new transaction
        } else {
            log.debug(
                "Completion status for membership ID {} is already {}",
                membershipId,
                newStatus,
            )
        }
    }

    /** Calculates the TaskCompletionStatus based on current state. */
    private fun calculateStatus(
        membership: TaskMembership,
        task: Task,
        submissions: List<TaskSubmission>, // Assumed sorted descending
        reviews: Map<IdType?, TaskSubmissionReview?>, // Map: SubmissionID -> Review
    ): TaskCompletionStatus {

        // 1. SUCCESS is final if any submission is accepted
        val hasAcceptedSubmission = submissions.any { sub -> reviews[sub.id]?.accepted == true }
        if (hasAcceptedSubmission) {
            return TaskCompletionStatus.SUCCESS
        }

        // Check deadline based on membership's specific deadline
        val submissionDeadline = membership.deadline
        val now = LocalDateTime.now()
        val deadlinePassed = submissionDeadline != null && submissionDeadline <= now

        // 2. Handle NOT_SUBMITTED and immediate FAILED case
        if (submissions.isEmpty()) {
            return if (deadlinePassed) TaskCompletionStatus.FAILED
            else TaskCompletionStatus.NOT_SUBMITTED
        }

        // 3. Analyze the latest submission
        val latestSubmission = submissions.first()
        val latestReview = reviews[latestSubmission.id]

        // 4. Latest submission reviewed (must be rejected, as SUCCESS handled above)
        if (latestReview != null) {
            // Rejected. Can it be resubmitted?
            return if (task.resubmittable && !deadlinePassed) {
                TaskCompletionStatus.REJECTED_RESUBMITTABLE
            } else {
                // Not resubmittable OR deadline passed after rejection
                TaskCompletionStatus.FAILED
            }
        }
        // 5. Latest submission pending review
        else {
            return if (deadlinePassed) {
                // Deadline passed while pending, no SUCCESS yet -> FAILED
                TaskCompletionStatus.FAILED
            } else {
                // Still time (or no deadline), waiting for review
                TaskCompletionStatus.PENDING_REVIEW
            }
        }
    }
}
