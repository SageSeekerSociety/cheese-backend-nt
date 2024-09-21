package org.rucca.cheese.task

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskDTO
import org.rucca.cheese.model.TaskSubmissionSchemaEntryDTO
import org.rucca.cheese.model.TaskSubmissionTypeDTO
import org.rucca.cheese.model.TaskSubmitterTypeDTO
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
class TaskService(
        private val userService: UserService,
        private val teamService: TeamService,
        private val taskRepository: TaskRepository,
        private val taskMembershipRepository: taskMembershipRepository,
) {
    fun getTaskDto(taskId: IdType): TaskDTO {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return TaskDTO(
                task.id!!,
                task.name!!,
                convertTaskSubmitterType(task.submitterType!!),
                userService.getUserDto(task.creator!!.id!!.toLong()),
                task.deadline!!.toLocalDate(),
                task.resubmittable!!,
                task.editable!!,
                task.description!!,
                task.submissionSchema!!
                        .sortedBy { it.index }
                        .map {
                            TaskSubmissionSchemaEntryDTO(it.description!!, convertTaskSubmissionEntryType(it.type!!))
                        })
    }

    fun getTaskOwner(taskId: IdType): IdType {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return task.creator!!.id!!.toLong()
    }

    fun isTaskParticipant(taskId: IdType, userId: IdType, memberId: IdType): Boolean {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                    userId == memberId && taskMembershipRepository.existsByTaskIdAndMemberId(taskId, memberId)
            TaskSubmitterType.TEAM ->
                    teamService.isTeamMember(memberId, userId) &&
                            taskMembershipRepository.existsByTaskIdAndMemberId(taskId, memberId)
        }
    }

    fun convertTaskSubmitterType(type: TaskSubmitterType): TaskSubmitterTypeDTO {
        return when (type) {
            TaskSubmitterType.USER -> TaskSubmitterTypeDTO.USER
            TaskSubmitterType.TEAM -> TaskSubmitterTypeDTO.TEAM
        }
    }

    fun convertTaskSubmissionEntryType(type: TaskSubmissionEntryType): TaskSubmissionTypeDTO {
        return when (type) {
            TaskSubmissionEntryType.TEXT -> TaskSubmissionTypeDTO.TEXT
            TaskSubmissionEntryType.ATTACHMENT -> TaskSubmissionTypeDTO.FILE
        }
    }
}
