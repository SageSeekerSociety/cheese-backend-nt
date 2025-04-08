package org.rucca.cheese.task.management

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.task.TaskMembershipService
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
class TaskAdminMBean(private val taskMembershipService: TaskMembershipService) {
    private val logger = LoggerFactory.getLogger(TaskAdminMBean::class.java)

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
}
