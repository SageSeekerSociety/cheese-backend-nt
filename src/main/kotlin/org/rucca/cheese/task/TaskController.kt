package org.rucca.cheese.task

import org.rucca.cheese.api.TasksApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TaskController : TasksApi {
    override fun deleteTask(taskId: Long): ResponseEntity<DeleteTask200ResponseDTO> {
        return super.deleteTask(taskId)
    }

    override fun deleteTaskMember(taskId: Long, member: Long): ResponseEntity<GetTask200ResponseDTO> {
        return super.deleteTaskMember(taskId, member)
    }

    override fun getTask(taskId: Long): ResponseEntity<GetTask200ResponseDTO> {
        return super.getTask(taskId)
    }

    override fun getTaskSubmissions(
            taskId: Long,
            user: Long?,
            version: Int?,
            pageSize: Int,
            pageStart: Int?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetTaskSubmissions200ResponseDTO> {
        return super.getTaskSubmissions(taskId, user, version, pageSize, pageStart, sortBy, sortOrder)
    }

    override fun getTasks(
            space: Long?,
            team: Int?,
            pageSize: Int,
            pageStart: Int?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetTasks200ResponseDTO> {
        return super.getTasks(space, team, pageSize, pageStart, sortBy, sortOrder)
    }

    override fun patchTask(
            taskId: Long,
            patchTaskRequestDTO: PatchTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.patchTask(taskId, patchTaskRequestDTO)
    }

    override fun patchTaskSubmission(
            taskId: Long,
            user: Long,
            version: Int,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>?
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        return super.patchTaskSubmission(taskId, user, version, postTaskSubmissionRequestInnerDTO)
    }

    override fun postTask(postTaskRequestDTO: PostTaskRequestDTO): ResponseEntity<GetTask200ResponseDTO> {
        return super.postTask(postTaskRequestDTO)
    }

    override fun postTaskMember(
            taskId: Long,
            postTaskMemberRequestDTO: PostTaskMemberRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.postTaskMember(taskId, postTaskMemberRequestDTO)
    }

    override fun postTaskSubmission(
            taskId: Long,
            user: Long,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>?
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        return super.postTaskSubmission(taskId, user, postTaskSubmissionRequestInnerDTO)
    }
}
