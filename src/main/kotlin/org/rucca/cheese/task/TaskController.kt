package org.rucca.cheese.task

import org.rucca.cheese.api.TaskApi
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TaskController : TaskApi {
    override fun deleteTask(task: Long): ResponseEntity<DeleteTask200ResponseDTO> {
        return super.deleteTask(task)
    }

    override fun deleteTaskMember(task: Long, member: Long): ResponseEntity<GetTask200ResponseDTO> {
        return super.deleteTaskMember(task, member)
    }

    override fun getTask(task: Long): ResponseEntity<GetTask200ResponseDTO> {
        return super.getTask(task)
    }

    override fun getTaskSubmissions(
            task: Long?,
            user: Long?,
            version: Int?,
            pageSize: Int,
            pageStart: Int?,
            sortBy: String,
            sortOrder: String
    ): ResponseEntity<GetTaskSubmissions200ResponseDTO> {
        return super.getTaskSubmissions(task, user, version, pageSize, pageStart, sortBy, sortOrder)
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
            task: Long,
            patchTaskRequestDTO: PatchTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.patchTask(task, patchTaskRequestDTO)
    }

    override fun patchTaskSubmission(
            task: Long,
            user: Long,
            version: Int,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>?
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        return super.patchTaskSubmission(task, user, version, postTaskSubmissionRequestInnerDTO)
    }

    override fun postTask(postTaskRequestDTO: PostTaskRequestDTO): ResponseEntity<GetTask200ResponseDTO> {
        return super.postTask(postTaskRequestDTO)
    }

    override fun postTaskMember(
            task: Long,
            postTaskMemberRequestDTO: PostTaskMemberRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.postTaskMember(task, postTaskMemberRequestDTO)
    }

    override fun postTaskSubmission(
            task: Long,
            user: Long,
            postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>?
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        return super.postTaskSubmission(task, user, postTaskSubmissionRequestInnerDTO)
    }
}
