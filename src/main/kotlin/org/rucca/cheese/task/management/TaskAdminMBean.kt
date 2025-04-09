package org.rucca.cheese.task.management

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.error.PreconditionFailedError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.task.TaskMembershipService
import org.rucca.cheese.task.TaskService
import org.slf4j.LoggerFactory
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedOperationParameter
import org.springframework.jmx.export.annotation.ManagedOperationParameters
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component

@Component
@ManagedResource(
    objectName = "org.rucca.cheese.task:type=TaskAdmin,name=TaskRealNameFixer",
    description = "MBean for task real name administrative operations",
)
class TaskAdminMBean(
    private val taskService: TaskService,
    private val taskMembershipService: TaskMembershipService,
) {
    private val logger = LoggerFactory.getLogger(TaskAdminMBean::class.java)
    // Basic flag to prevent concurrent runs of the snapshot task via JMX
    private val isSnapshotTaskRunning = AtomicBoolean(false)
    // Dedicated executor for potentially long-running tasks triggered via JMX
    private val backgroundExecutor: ExecutorService =
        Executors.newSingleThreadExecutor { r ->
            val t = Thread(r, "jmx-task-admin-worker")
            t.isDaemon = true // Allow JVM to exit even if this thread is running
            t
        }

    @ManagedOperation(
        description = "Fix missing real name info for participants of a specific task"
    )
    @ManagedOperationParameters(
        ManagedOperationParameter(name = "taskId", description = "The ID of the task to fix")
    )
    fun fixMissingRealNameInfo(taskId: IdType): String {
        logger.info("[JMX] Operation triggered: fix real name info for task ID: {}", taskId)

        return try {
            val updatedCount = taskMembershipService.fixRealNameInfoForTask(taskId)
            val message =
                "Successfully fixed real name info for task ID: $taskId. Updated memberships: $updatedCount"
            logger.info("[JMX] {}", message)
            "SUCCESS: $message"
        } catch (e: Exception) {
            logger.error(
                "[JMX] Error fixing real name info for task ID: {}: {}",
                taskId,
                e.message,
                e,
            )
            "ERROR: Failed to fix real name info for task ID $taskId. Reason: ${e.message}"
        }
    }

    @ManagedOperation(
        description =
            "Create missing team member snapshots for older team participations (runs asynchronously)."
    )
    // No parameters needed for this operation
    fun createMissingTeamSnapshots(): String {
        logger.info("[JMX] Operation triggered: create missing team snapshots.")

        // Prevent concurrent execution via JMX
        if (isSnapshotTaskRunning.compareAndSet(false, true)) {
            try {
                // Submit the task to run in the background
                CompletableFuture.runAsync(
                    {
                        try {
                            logger.info("[JMX BG Task] Starting missing snapshot creation...")
                            val result =
                                taskMembershipService.createMissingTeamSnapshotsForAllTasks()
                            logger.info(
                                "[JMX BG Task] Missing snapshot creation finished. Result: {}",
                                result,
                            )
                        } catch (e: Exception) {
                            logger.error(
                                "[JMX BG Task] Error during background snapshot creation.",
                                e,
                            )
                        } finally {
                            isSnapshotTaskRunning.set(false) // Release the lock
                            logger.info("[JMX BG Task] Lock released.")
                        }
                    },
                    backgroundExecutor,
                )

                val message =
                    "Snapshot creation task started asynchronously. Check server logs for progress and results."
                logger.info("[JMX] {}", message)
                return "SUCCESS: $message"
            } catch (e: Exception) {
                // Handle potential errors during submission itself
                logger.error("[JMX] Error submitting snapshot creation task.", e)
                isSnapshotTaskRunning.set(false) // Ensure lock is released on submission error
                return "ERROR: Failed to start snapshot creation task. Reason: ${e.message}"
            }
        } else {
            val message =
                "Snapshot creation task is already running. Please wait for it to complete."
            logger.warn("[JMX] {}", message)
            return "WARN: $message"
        }
    }

    @ManagedOperation(
        description = "Check if the missing snapshot creation task is currently running."
    )
    fun isSnapshotCreationRunning(): Boolean {
        return isSnapshotTaskRunning.get()
    }

    @ManagedOperation(description = "Enable the 'Require Real Name' setting for a specific task.")
    @ManagedOperationParameters(
        ManagedOperationParameter(name = "taskId", description = "The ID of the task to modify")
    )
    fun enableTaskRealNameRequirement(taskId: IdType): String {
        logger.info("[JMX] Operation triggered: Enable 'Require Real Name' for task ID: {}", taskId)
        return try {
            taskService.enableRealNameRequirement(taskId) // Call the service method
            val message = "Successfully enabled 'Require Real Name' for task ID: $taskId."
            logger.info("[JMX] {}", message)
            "SUCCESS: $message"
        } catch (e: PreconditionFailedError) {
            // Catch specific error for conditions not met (already enabled, missing info)
            logger.warn(
                "[JMX] Precondition failed for enabling real name on task {}: {}",
                taskId,
                e.message,
            )
            "WARN: ${e.message}" // Return the specific reason
        } catch (e: NotFoundError) {
            logger.error("[JMX] Task not found when trying to enable real name: {}", taskId, e)
            "ERROR: Task not found with ID $taskId."
        } catch (e: Exception) {
            logger.error("[JMX] Error enabling real name for task ID: {}: {}", taskId, e.message, e)
            "ERROR: Failed to enable real name for task ID $taskId. Reason: ${e.message}"
        }
    }

    @ManagedOperation(description = "Disable the 'Require Real Name' setting for a specific task.")
    @ManagedOperationParameters(
        ManagedOperationParameter(name = "taskId", description = "The ID of the task to modify")
    )
    fun disableTaskRealNameRequirement(taskId: IdType): String {
        logger.info(
            "[JMX] Operation triggered: Disable 'Require Real Name' for task ID: {}",
            taskId,
        )
        return try {
            taskService.disableRealNameRequirement(taskId) // Call the service method
            val message = "Successfully disabled 'Require Real Name' for task ID: $taskId."
            logger.info("[JMX] {}", message)
            "SUCCESS: $message"
        } catch (e: PreconditionFailedError) {
            logger.warn(
                "[JMX] Precondition failed for disabling real name on task {}: {}",
                taskId,
                e.message,
            )
            "WARN: ${e.message}" // Return the specific reason (e.g., already disabled)
        } catch (e: NotFoundError) {
            logger.error("[JMX] Task not found when trying to disable real name: {}", taskId, e)
            "ERROR: Task not found with ID $taskId."
        } catch (e: Exception) {
            logger.error(
                "[JMX] Error disabling real name for task ID: {}: {}",
                taskId,
                e.message,
                e,
            )
            "ERROR: Failed to disable real name for task ID $taskId. Reason: ${e.message}"
        }
    }
}
