package org.rucca.cheese.project

import org.hibernate.query.SortDirection
import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(private val projectService: ProjectService) : ProjectsApi {
    @Guard("create", "project")
    override fun createProject(
        createProjectRequestDTO: CreateProjectRequestDTO
    ): ResponseEntity<CreateProject200ResponseDTO> {
        val projectId =
            projectService.createProject(
                name = createProjectRequestDTO.name,
                description = createProjectRequestDTO.description,
                colorCode = createProjectRequestDTO.colorCode,
                startDate = createProjectRequestDTO.startDate.toLocalDateTime(),
                endDate = createProjectRequestDTO.endDate.toLocalDateTime(),
                teamId = createProjectRequestDTO.teamId,
                leaderId = createProjectRequestDTO.leaderId,
                content = createProjectRequestDTO.content,
                parentId = createProjectRequestDTO.parentId,
                externalTaskId = createProjectRequestDTO.externalTaskId,
                githubRepo = createProjectRequestDTO.githubRepo,
            )
        val projectDto = projectService.getProjectDto(projectId)
        return ResponseEntity.ok(
            CreateProject200ResponseDTO(data = CreateProject200ResponseDataDTO(projectDto))
        )
    }

    @Guard("enumerate", "project")
    override fun getProjects(
        parentId: Long?,
        leaderId: Long?,
        memberId: Long?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<GetProjects200ResponseDTO> {
        val sortBy = ProjectService.ProjectsSortBy.CREATED_AT // Default sort by createdAt
        val sortOrder = SortDirection.ASCENDING // Default sort order

        val (projects, page) =
            projectService.enumerateProjects(
                parentId = parentId,
                leaderId = leaderId,
                memberId = memberId,
                sortBy = sortBy,
                sortOrder = sortOrder,
                pageSize = pageSize,
                pageStart = pageStart,
            )

        return ResponseEntity.ok(
            GetProjects200ResponseDTO(data = GetProjects200ResponseDataDTO(projects, page))
        )
    }
}
