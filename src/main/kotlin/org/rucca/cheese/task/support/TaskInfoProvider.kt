package org.rucca.cheese.task.support

import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.task.TaskRepository
import org.springframework.stereotype.Service

/**
 * Interface providing minimal Task information needed by other services, avoiding full TaskService
 * dependency.
 */
interface TaskInfoProvider {
    fun getTaskNameById(taskId: IdType): String? // Returns null if not found or name not available
}

@Service
class TaskInfoProviderImpl(private val taskRepository: TaskRepository) : TaskInfoProvider {
    override fun getTaskNameById(taskId: IdType): String? {
        return taskRepository.findById(taskId).map { it.name }.orElse(null)
    }
}
