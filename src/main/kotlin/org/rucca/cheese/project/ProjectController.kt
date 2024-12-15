package org.rucca.cheese.project

import javax.annotation.PostConstruct
import org.hibernate.query.SortDirection
import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.api.TasksApi
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.model.*
import org.rucca.cheese.project.ProjectService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*



@RestController
class ProjectController(
    private val projectService: ProjectService
    ) : ProjectsApi {
    /*@PostConstruct
    fun initialize() {

    }*/

    @Guard("create", "project")
    override fun createProject(
        postProjectRequestDTO: PostProjectRequestDTO
    ): ResponseEntity<GetProject200ResponseDTO> {
        projectService.createProject(postProjectRequestDTO)
        return ResponseEntity.ok(GetProject200ResponseDTO(200, "OK"))
    }

    @Guard("enumerate", "project")
    override fun enumerateSpaces(
        parentId: Long?,
        leaderId: Long?,
        memberId: Long?,
        status: Int?,
        pageSize: Int?,
        pageStart: Long?,
    ): ResponseEntity<GetProjects200ResponseDTO> {
        projectService.enumerateSpaces(
            parentId,
            leaderId,
            memberId,
            status,
            pageSize,
            pageStart
        )
        return ResponseEntity.ok(GetProjects200ResponseDTO(200, "OK"))
    }
}
