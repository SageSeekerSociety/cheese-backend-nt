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

    override fun deleteTaskMember(task: Long, memberType: String, member: Long): ResponseEntity<GetTask200ResponseDTO> {
        return super.deleteTaskMember(task, memberType, member)
    }

    override fun getTask(task: Long): ResponseEntity<GetTask200ResponseDTO> {
        return super.getTask(task)
    }

    override fun patchTask(
            task: Long,
            patchTaskRequestDTO: PatchTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.patchTask(task, patchTaskRequestDTO)
    }

    override fun patchTaskMember(
            task: Long,
            memberType: String,
            member: Long,
            patchTaskMemberRequestDTO: PatchTaskMemberRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        return super.patchTaskMember(task, memberType, member, patchTaskMemberRequestDTO)
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
}
