package org.rucca.cheese.task

import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
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
        private val taskSubmissionRepository: taskSubmissionRepository,
        private val entityManager: EntityManager,
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
                                team = if (teamId != null) Team().apply { id = teamId } else null,
                                space = if (spaceId != null) Space().apply { id = spaceId } else null,
                                description = description,
                                submissionSchema = submissionSchema))
        return task.id!!
    }

    fun updateTaskName(taskId: IdType, name: String) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.name = name
        taskRepository.save(task)
    }

    fun updateTaskDeadline(taskId: IdType, deadline: LocalDate) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.deadline = deadline
        taskRepository.save(task)
    }

    fun updateTaskResubmittable(taskId: IdType, resubmittable: Boolean) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.resubmittable = resubmittable
        taskRepository.save(task)
    }

    fun updateTaskEditable(taskId: IdType, editable: Boolean) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.editable = editable
        taskRepository.save(task)
    }

    fun updateTaskDescription(taskId: IdType, description: String) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.description = description
        taskRepository.save(task)
    }

    fun updateTaskSubmissionSchema(taskId: IdType, submissionSchema: List<TaskSubmissionSchema>) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.submissionSchema = submissionSchema
        taskRepository.save(task)
    }

    enum class TasksSortBy {
        DEADLINE,
        CREATED_AT,
        UPDATED_AT,
    }

    fun getTaskSumbitterType(taskId: IdType): TaskSubmitterTypeDTO {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return convertTaskSubmitterType(task.submitterType!!)
    }

    fun getTaskSubmitterSummary(taskId: IdType): TaskSummarySubmittersDTO {
        val submitters = taskMembershipRepository.findByTaskIdWhereMemberHasSubmitted(taskId)
        val examples = submitters.sortedBy { it.updatedAt }.reversed().take(3)
        val exampleDTOs =
                when (getTaskSumbitterType(taskId)) {
                    TaskSubmitterTypeDTO.USER -> examples.map { userService.getUserAvatarId(it.memberId!!) }
                    TaskSubmitterTypeDTO.TEAM -> examples.map { teamService.getTeamAvatarId(it.memberId!!) }
                }.map { TaskSummarySubmittersExamplesInnerDTO(it) }
        return TaskSummarySubmittersDTO(total = submitters.size, examples = exampleDTOs)
    }

    fun enumerateTasks(
            space: IdType?,
            team: Int?,
            pageSize: Int,
            pageStart: IdType?,
            sortBy: TasksSortBy,
            sortOrder: SortDirection,
    ): Pair<List<TaskSummaryDTO>, PageDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Task::class.java)
        val root = cq.from(Task::class.java)
        if (space != null) {
            cq.where(cb.equal(root.get<Space>("space").get<IdType>("id"), space))
        }
        if (team != null) {
            cq.where(cb.equal(root.get<Team>("team").get<IdType>("id"), team))
        }
        val by =
                when (sortBy) {
                    TasksSortBy.CREATED_AT -> root.get<LocalDateTime>("createdAt")
                    TasksSortBy.UPDATED_AT -> root.get<LocalDateTime>("updatedAt")
                    TasksSortBy.DEADLINE -> root.get<LocalDate>("deadline")
                }
        val order =
                when (sortOrder) {
                    SortDirection.ASCENDING -> cb.asc(by)
                    SortDirection.DESCENDING -> cb.desc(by)
                }
        cq.orderBy(order)
        val query = entityManager.createQuery(cq)
        val result = query.resultList
        val (curr, page) =
                PageHelper.pageFromAll(
                        result, pageStart, pageSize, { it.id!! }, { id -> throw NotFoundError("task", id) })
        return Pair(
                curr.map {
                    TaskSummaryDTO(
                            deadline = it.deadline!!,
                            id = it.id!!,
                            intro = it.description!!,
                            name = it.name!!,
                            submitters = getTaskSubmitterSummary(it.id!!))
                },
                page)
    }
}
