package cn.edu.ruc.cheese.backend.task

import cn.edu.ruc.cheese.backend.api.TaskApi
import cn.edu.ruc.cheese.backend.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TaskController : TaskApi {
    override fun deleteTask(task: Int): ResponseEntity<DeleteTask200Response> {
        return super.deleteTask(task)
    }

    override fun deleteTaskMember(task: Int, memberType: String, member: Int): ResponseEntity<GetTask200Response> {
        return super.deleteTaskMember(task, memberType, member)
    }

    override fun getTask(task: Int): ResponseEntity<GetTask200Response> {
        return super.getTask(task)
    }

    override fun patchTask(task: Int, patchTaskRequest: PatchTaskRequest?): ResponseEntity<GetTask200Response> {
        return super.patchTask(task, patchTaskRequest)
    }

    override fun patchTaskMember(
            task: Int,
            memberType: String,
            member: Int,
            patchTaskMemberRequest: PatchTaskMemberRequest?
    ): ResponseEntity<GetTask200Response> {
        return super.patchTaskMember(task, memberType, member, patchTaskMemberRequest)
    }

    override fun postTask(postTaskRequest: PostTaskRequest?): ResponseEntity<GetTask200Response> {
        return super.postTask(postTaskRequest)
    }

    override fun postTaskMember(
            task: Int,
            postTaskMemberRequest: PostTaskMemberRequest?
    ): ResponseEntity<GetTask200Response> {
        return super.postTaskMember(task, postTaskMemberRequest)
    }
}
