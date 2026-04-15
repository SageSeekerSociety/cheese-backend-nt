package org.rucca.cheese.task.error

import java.time.Instant
import java.time.ZoneId
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.persistent.IdType

class TaskRegistrationNotStartedError(
    taskId: IdType,
    registrationStartAtEpochMillis: Long,
    zoneId: String,
) : ForbiddenError(buildMessage(taskId, registrationStartAtEpochMillis, zoneId)) {
    companion object {
        private fun buildMessage(
            taskId: IdType,
            registrationStartAtEpochMillis: Long,
            zoneId: String,
        ): String {
            val zone = runCatching { ZoneId.of(zoneId) }.getOrElse { ZoneId.systemDefault() }
            val formatted = Instant.ofEpochMilli(registrationStartAtEpochMillis).atZone(zone)
            return "Task $taskId registration opens at $formatted."
        }
    }
}
