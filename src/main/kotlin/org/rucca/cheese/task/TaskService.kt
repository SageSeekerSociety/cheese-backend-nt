package org.rucca.cheese.task

import java.time.LocalDate
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.TaskDTO
import org.rucca.cheese.model.TaskSubmissionSchemaEntryDTO
import org.rucca.cheese.model.TaskSubmissionTypeDTO
import org.rucca.cheese.model.TaskSubmitterTypeDTO
import org.rucca.cheese.space.Space
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.User
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
                task.deadline!!,
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

    fun convertTaskSubmitterType(type: TaskSubmitterTypeDTO): TaskSubmitterType {
        return when (type) {
            TaskSubmitterTypeDTO.USER -> TaskSubmitterType.USER
            TaskSubmitterTypeDTO.TEAM -> TaskSubmitterType.TEAM
        }
    }

    fun convertTaskSubmissionEntryType(type: TaskSubmissionEntryType): TaskSubmissionTypeDTO {
        return when (type) {
            TaskSubmissionEntryType.TEXT -> TaskSubmissionTypeDTO.TEXT
            TaskSubmissionEntryType.ATTACHMENT -> TaskSubmissionTypeDTO.FILE
        }
    }

    fun convertTaskSubmissionEntryType(type: TaskSubmissionTypeDTO): TaskSubmissionEntryType {
        return when (type) {
            TaskSubmissionTypeDTO.TEXT -> TaskSubmissionEntryType.TEXT
            TaskSubmissionTypeDTO.FILE -> TaskSubmissionEntryType.ATTACHMENT
        }
    }

    fun createTask(
            name: String,
            submitterType: TaskSubmitterType,
            deadline: LocalDate,
            resubmittable: Boolean,
            editable: Boolean,
            description: String,
            submissionSchema: List<TaskSubmissionSchema>,
            creatorId: IdType,
            teamId: IdType? = null,
            spaceId: IdType? = null,
    ): IdType {
        val task =
                taskRepository.save(
                        Task(
                                name = name,
                                submitterType = submitterType,
                                creator = User().apply { id = creatorId.toInt() },
                                deadline = deadline,
                                resubmittable = resubmittable,
                                editable = editable,
                                team = Team().apply { id = teamId },
                                space = Space().apply { id = spaceId },
                                description = description,
                                submissionSchema = submissionSchema))
        return task.id!!
    }
}
