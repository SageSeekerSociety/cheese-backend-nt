package cn.edu.ruc.cheese.backend.task

import cn.edu.ruc.cheese.backend.api.TaskApi
import cn.edu.ruc.cheese.backend.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TaskController : TaskApi {
    override fun deleteTask(task: Int?): ResponseEntity<InlineResponse2003> {
        return super.deleteTask(task)
    }

    override fun deleteTaskMember(task: Int?, memberType: String?, member: Int?): ResponseEntity<InlineResponse2002> {
        return super.deleteTaskMember(task, memberType, member)
    }

    override fun getTask(task: Int?): ResponseEntity<InlineResponse2002> {
        return super.getTask(task)
    }

    override fun patchTask(task: Int?, body: TaskBody1?): ResponseEntity<InlineResponse2002> {
        return super.patchTask(task, body)
    }

    override fun patchTaskMember(
            task: Int?,
            memberType: String?,
            member: Int?,
            body: TaskMemberBody1?
    ): ResponseEntity<InlineResponse2002> {
        return super.patchTaskMember(task, memberType, member, body)
    }

    override fun postTask(body: TaskBody?): ResponseEntity<InlineResponse2002> {
        return super.postTask(body)
    }

    override fun postTaskMember(task: Int?, body: TaskMemberBody?): ResponseEntity<InlineResponse2002> {
        return super.postTaskMember(task, body)
    }
}
