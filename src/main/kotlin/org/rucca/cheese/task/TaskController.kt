package org.rucca.cheese.task

import javax.annotation.PostConstruct
import org.rucca.cheese.api.TasksApi
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.AuthInfo
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TaskController(
        private val taskService: TaskService,
        private val authorizationService: AuthorizationService,
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
            taskService.isTaskParticipant(
                    resourceId ?: throw IllegalArgumentException("resourceId is null"),
                    userId,
                    authInfo["member"] as? IdType ?: throw IllegalArgumentException("member is null"))
        }
    }

    @Guard("delete", "task")
    override fun deleteTask(@ResourceId taskId: Long): ResponseEntity<DeleteTask200ResponseDTO> {
        return super.deleteTask(taskId)
    }

    @Guard("remove-member", "task")
    override fun deleteTaskMember(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.deleteTaskMember(taskId, member)
    }

    @Guard("query", "task")
    override fun getTask(@ResourceId taskId: Long): ResponseEntity<GetTask200ResponseDTO> {
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK"))
    }

    @Guard("enumerate-submissions", "task")
    override fun getTaskSubmissions(
            @ResourceId taskId: Long,
            member: Long?,
            version: Int?,
            pageSize: Int,
            pageStart: Long?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetTaskSubmissions200ResponseDTO> {
        return super.getTaskSubmissions(taskId, member, version, pageSize, pageStart, sortBy, sortOrder)
    }

    @Guard("enumerate", "task")
    override fun getTasks(
            space: Long?,
            team: Int?,
            pageSize: Int,
            pageStart: Long?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetTasks200ResponseDTO> {
        return super.getTasks(space, team, pageSize, pageStart, sortBy, sortOrder)
    }

    @Guard("modify", "task")
    override fun patchTask(
            @ResourceId taskId: Long,
            patchTaskRequestDTO: PatchTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.patchTask(taskId, patchTaskRequestDTO)
    }

    @Guard("modify-submission", "task")
    override fun patchTaskSubmission(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long,
            version: Int,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>?
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        return super.patchTaskSubmission(taskId, member, version, postTaskSubmissionRequestInnerDTO)
    }

    @Guard("create", "task")
    override fun postTask(postTaskRequestDTO: PostTaskRequestDTO): ResponseEntity<GetTask200ResponseDTO> {
        return super.postTask(postTaskRequestDTO)
    }

    @Guard("add-member", "task")
    override fun postTaskMember(
            @ResourceId taskId: Long,
            postTaskMemberRequestDTO: PostTaskMemberRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.postTaskMember(taskId, postTaskMemberRequestDTO)
    }

    @Guard("submit", "task")
    override fun postTaskSubmission(
            @ResourceId taskId: Long,
            @AuthInfo("member") member: Long,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>?
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        return super.postTaskSubmission(taskId, member, postTaskSubmissionRequestInnerDTO)
    }
}
