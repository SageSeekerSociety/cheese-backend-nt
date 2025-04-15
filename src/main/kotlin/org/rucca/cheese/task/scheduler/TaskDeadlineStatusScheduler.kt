package org.rucca.cheese.task.scheduler

import java.time.LocalDateTime
import org.rucca.cheese.task.TaskCompletionStatus
import org.rucca.cheese.task.TaskMembershipRepository
import org.rucca.cheese.task.service.TaskMembershipStatusService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TaskDeadlineStatusScheduler(
    private val taskMembershipRepository: TaskMembershipRepository,
    private val statusService: TaskMembershipStatusService,
) {

    private val log = LoggerFactory.getLogger(TaskDeadlineStatusScheduler::class.java)

    @Scheduled(cron = "0 */15 * * * *")
    fun checkDeadlinesAndFailMemberships() {
        log.info("Running scheduled task to check for passed submission deadlines...")
        val now = LocalDateTime.now()

        // Statuses that might transition to FAILED due to deadline passing
        val potentiallyAffectedStatuses =
            listOf(
                TaskCompletionStatus.PENDING_REVIEW,
                TaskCompletionStatus.REJECTED_RESUBMITTABLE,
                TaskCompletionStatus.NOT_SUBMITTED,
            )

        val pageSize = 100
        var pageNumber = 0
        var updatedCount = 0

        do {
            val pageable = PageRequest.of(pageNumber, pageSize)
            // Find memberships in relevant states that *have* a deadline set
            val page =
                taskMembershipRepository.findByCompletionStatusInAndDeadlineNotNull(
                    potentiallyAffectedStatuses,
                    pageable,
                )

            page.content.forEach { membership ->
                val submissionDeadline = membership.deadline // Use the correct deadline
                // Double check deadline isn't null and is in the past
                if (submissionDeadline != null && submissionDeadline <= now) {
                    log.debug(
                        "Scheduler: Deadline {} passed for membership {} (status {}). Triggering update.",
                        submissionDeadline,
                        membership.id,
                        membership.completionStatus,
                    )
                    try {
                        // Trigger update. The service logic will determine the new state (likely
                        // FAILED).
                        statusService.updateCompletionStatus(membership.id!!) // Uses REQUIRES_NEW
                        updatedCount++ // Count triggered updates, not necessarily successful ones
                        // here
                    } catch (e: Exception) {
                        log.error(
                            "Scheduler: Error updating status for membership {}: {}",
                            membership.id,
                            e.message,
                            e,
                        )
                        // Handle error (e.g., log and continue)
                    }
                }
            }
            pageNumber++
        } while (page.hasNext())

        if (updatedCount > 0) {
            log.info(
                "Scheduled deadline check finished. Triggered status updates for {} memberships.",
                updatedCount,
            )
        } else {
            log.debug(
                "Scheduled deadline check finished. No memberships required status update due to deadline."
            )
        }
    }
}
