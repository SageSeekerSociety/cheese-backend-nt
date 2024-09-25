package org.rucca.cheese.task

import javax.annotation.PostConstruct
import org.hibernate.query.SortDirection
import org.rucca.cheese.api.TasksApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.AuthInfo
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.helper.toEntryList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TaskController(
        private val taskService: TaskService,
        private val authorizationService: AuthorizationService,
        private val authenticationService: AuthenticationService,
) : TasksApi {
    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register("task", taskService::getTaskOwner)
        authorizationService.customAuthLogics.register("is-task-participant") {
                userId: IdType,
                _: AuthorizedAction,
                _: String,
                resourceId: IdType?,
                authInfo: Map<String, Any>,
                _: IdGetter?,
                _: Any?,
            ->
            val memberId = authInfo["member"] as? IdType
            if (resourceId == null || memberId == null) {
                false
            } else {
                taskService.isTaskParticipant(resourceId, userId, memberId)
            }
        }
        authorizationService.customAuthLogics.register("participant-is-self-or-team-where-i-am-admin") {
                userId: IdType,
                _: AuthorizedAction,
                _: String,
                resourceId: IdType?,
                authInfo: Map<String, Any>,
                _: IdGetter?,
                _: Any?,
            ->
            val memberId = authInfo["member"] as? IdType
            if (resourceId == null || memberId == null) {
                false
            } else {
                taskService.participantIsSelfOrTeamWhereIAmAdmin(resourceId, userId, memberId)
            }
        }
    }

    @Guard("delete", "task")
    override fun deleteTask(@ResourceId taskId: Long): ResponseEntity<DeleteTask200ResponseDTO> {
        taskService.deleteTask(taskId)
        return ResponseEntity.ok(DeleteTask200ResponseDTO(200, "OK"))
    }

    @Guard("remove-participant", "task")
    override fun deleteTaskParticipant(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long
    ): ResponseEntity<GetTask200ResponseDTO> {
        taskService.removeTaskParticipant(taskId, member)
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK"))
    }

    @Guard("query", "task")
    override fun getTask(@ResourceId taskId: Long, queryJoinability: Boolean): ResponseEntity<GetTask200ResponseDTO> {
        val taskDTO = taskService.getTaskDto(taskId, queryJoinability)
        return ResponseEntity.ok(GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK"))
    }

    @Guard("enumerate-participants", "task")
    override fun getTaskParticipants(@ResourceId taskId: Long): ResponseEntity<GetTaskParticipants200ResponseDTO> {
        val participants = taskService.getTaskParticipantDtos(taskId)
        return ResponseEntity.ok(
                GetTaskParticipants200ResponseDTO(200, GetTaskParticipants200ResponseDataDTO(participants), "OK"))
    }

    @Guard("enumerate-submissions", "task")
    override fun getTaskSubmissions(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long?,
            allVersions: Boolean,
            pageSize: Int,
            pageStart: Long?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetTaskSubmissions200ResponseDTO> {
        val by =
                when (sortBy) {
                    "createdAt" -> TaskService.TaskSubmissionSortBy.CREATED_AT
                    "updatedAt" -> TaskService.TaskSubmissionSortBy.UPDATED_AT
                    else -> throw IllegalArgumentException("Invalid sortBy: $sortBy")
                }
        val order =
                when (sortOrder) {
                    "asc" -> SortDirection.ASCENDING
                    "desc" -> SortDirection.DESCENDING
                    else -> throw IllegalArgumentException("Invalid sortOrder: $sortOrder")
                }
        val (dtos, page) =
                taskService.enumerateSubmissions(
                        taskId = taskId,
                        member = member,
                        allVersions = allVersions,
                        pageSize = pageSize,
                        pageStart = pageStart,
                        sortBy = by,
                        sortOrder = order)
        return ResponseEntity.ok(
                GetTaskSubmissions200ResponseDTO(200, GetTaskSubmissions200ResponseDataDTO(dtos, page), "OK"))
    }

    @Guard("enumerate", "task")
    override fun getTasks(
            space: Long?,
            team: Int?,
            pageSize: Int,
            pageStart: Long?,
            sortBy: String,
            sortOrder: String,
            queryJoinability: Boolean,
            keywords: String?,
    ): ResponseEntity<GetTasks200ResponseDTO> {
        val by =
                when (sortBy) {
                    "createdAt" -> TaskService.TasksSortBy.CREATED_AT
                    "updatedAt" -> TaskService.TasksSortBy.UPDATED_AT
                    "deadline" -> TaskService.TasksSortBy.DEADLINE
                    else -> throw IllegalArgumentException("Invalid sortBy: $sortBy")
                }
        val order =
                when (sortOrder) {
                    "asc" -> SortDirection.ASCENDING
                    "desc" -> SortDirection.DESCENDING
                    else -> throw IllegalArgumentException("Invalid sortOrder: $sortOrder")
                }
        val (taskSummaryDTOs, page) =
                taskService.enumerateTasks(
                        space = space,
                        team = team,
                        keywords = keywords,
                        pageSize = pageSize,
                        pageStart = pageStart,
                        sortBy = by,
                        sortOrder = order,
                        queryJoinability = queryJoinability)
        return ResponseEntity.ok(GetTasks200ResponseDTO(200, GetTasks200ResponseDataDTO(taskSummaryDTOs, page), "OK"))
    }

    @Guard("modify", "task")
    override fun patchTask(
            @ResourceId taskId: Long,
            patchTaskRequestDTO: PatchTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        if (patchTaskRequestDTO.name != null) {
            taskService.updateTaskName(taskId, patchTaskRequestDTO.name)
        }
        if (patchTaskRequestDTO.deadline != null) {
            taskService.updateTaskDeadline(taskId, patchTaskRequestDTO.deadline.toLocalDateTime())
        }
        if (patchTaskRequestDTO.resubmittable != null) {
            taskService.updateTaskResubmittable(taskId, patchTaskRequestDTO.resubmittable)
        }
        if (patchTaskRequestDTO.editable != null) {
            taskService.updateTaskEditable(taskId, patchTaskRequestDTO.editable)
        }
        if (patchTaskRequestDTO.description != null) {
            taskService.updateTaskDescription(taskId, patchTaskRequestDTO.description)
        }
        if (patchTaskRequestDTO.submissionSchema != null) {
            taskService.updateTaskSubmissionSchema(
                    taskId,
                    patchTaskRequestDTO.submissionSchema.withIndex().map {
                        TaskSubmissionSchema(
                                it.index, it.value.prompt, taskService.convertTaskSubmissionEntryType(it.value.type))
                    })
        }
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK"))
    }

    @Guard("modify-submission", "task")
    override fun patchTaskSubmission(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long,
            version: Int,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        val contents = postTaskSubmissionRequestInnerDTO.toEntryList()
        val submissions =
                taskService.modifySubmission(
                        taskId, member, authenticationService.getCurrentUserId(), version, contents)
        return ResponseEntity.ok(
                PostTaskSubmission200ResponseDTO(200, PostTaskSubmission200ResponseDataDTO(submissions), "OK"))
    }

    @Guard("create", "task")
    override fun postTask(postTaskRequestDTO: PostTaskRequestDTO): ResponseEntity<GetTask200ResponseDTO> {
        val taskId =
                taskService.createTask(
                        name = postTaskRequestDTO.name,
                        submitterType = taskService.convertTaskSubmitterType(postTaskRequestDTO.submitterType),
                        deadline = postTaskRequestDTO.deadline.toLocalDateTime(),
                        resubmittable = postTaskRequestDTO.resubmittable,
                        editable = postTaskRequestDTO.editable,
                        description = postTaskRequestDTO.description,
                        submissionSchema =
                                postTaskRequestDTO.submissionSchema.withIndex().map {
                                    TaskSubmissionSchema(
                                            it.index,
                                            it.value.prompt,
                                            taskService.convertTaskSubmissionEntryType(it.value.type))
                                },
                        creatorId = authenticationService.getCurrentUserId(),
                        teamId = postTaskRequestDTO.team,
                        spaceId = postTaskRequestDTO.space)
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK"))
    }

    @Guard("add-participant", "task")
    override fun postTaskParticipant(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long
    ): ResponseEntity<GetTask200ResponseDTO> {
        taskService.addTaskParticipant(taskId, member)
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK"))
    }

    @Guard("submit", "task")
    override fun postTaskSubmission(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        val contents = postTaskSubmissionRequestInnerDTO.toEntryList()
        val submissions = taskService.submitTask(taskId, member, authenticationService.getCurrentUserId(), contents)
        return ResponseEntity.ok(
                PostTaskSubmission200ResponseDTO(200, PostTaskSubmission200ResponseDataDTO(submissions), "OK"))
    }
}
