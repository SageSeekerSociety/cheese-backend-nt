package org.rucca.cheese.task.listener

import org.rucca.cheese.task.event.TaskMembershipStatusUpdateEvent
import org.rucca.cheese.task.service.TaskMembershipStatusService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TaskStatusEventHandler(private val statusService: TaskMembershipStatusService) {
    private val log = LoggerFactory.getLogger(TaskStatusEventHandler::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleMembershipStatusUpdate(event: TaskMembershipStatusUpdateEvent) {
        log.info("Received status update event for membership ID: {}", event.membershipId)
        try {
            statusService.updateCompletionStatus(event.membershipId)
        } catch (e: Exception) {
            log.error(
                "Error handling membership status update event for ID {}: {}",
                event.membershipId,
                e.message,
                e,
            )
        }
    }
}
