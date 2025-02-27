package org.rucca.cheese.project

import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(private val projectService: ProjectService) : ProjectsApi {
    @Guard("create", "project")
    override fun projectsPost(
        projectsPostRequestDTO: ProjectsPostRequestDTO
    ): ResponseEntity<ProjectsPost200ResponseDTO> {
        val project = projectService.createProject(projectsPostRequestDTO)
        val projectDTO =
            ProjectDTO(
                id = project.id!!,
                name = project.name!!,
                description = project.description!!,
                startDate = project.startDate!!.toEpochMilli(),
                endDate = project.endDate!!.toEpochMilli(),
                leaderId = project.leader!!.id!!.toLong(),
                content = project.content!!,
                colorCode = project.colorCode,
                parentId = project.parent?.id,
                externalTaskId = project.externalTaskId,
                githubRepo = project.githubRepo,
                // path = project.path,
                createdAt = project.createdAt!!.toEpochMilli(),
                updatedAt = project.updatedAt!!.toEpochMilli(),
            )
        val responseData = ProjectsPost200ResponseDataDTO(projectDTO)
        return ResponseEntity.ok(ProjectsPost200ResponseDTO(data = responseData))
    }

    @Guard("query", "project")
    override fun projectsGet(
        parentId: Long?,
        leaderId: Long?,
        memberId: Long?,
        status: String?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ProjectsGet200ResponseDTO> {
        val projects =
            projectService.getProjects(parentId, leaderId, memberId, status, pageStart, pageSize)
        val projectDTOs =
            projects.map { project ->
                ProjectDTO(
                    id = project.id!!,
                    name = project.name!!,
                    description = project.description!!,
                    startDate = project.startDate!!.toEpochMilli(),
                    endDate = project.endDate!!.toEpochMilli(),
                    leaderId = project.leader!!.id!!.toLong(),
                    content = project.content!!,
                    colorCode = project.colorCode,
                    parentId = project.parent?.id,
                    externalTaskId = project.externalTaskId,
                    githubRepo = project.githubRepo,
                    // path = project.path,
                    createdAt = project.createdAt!!.toEpochMilli(),
                    updatedAt = project.updatedAt!!.toEpochMilli(),
                )
            }

        val (pageData, page) =
            PageHelper.pageFromAll(
                projectDTOs,
                pageStart,
                pageSize,
                { it.id },
                { throw NotFoundError("project", it) },
            )
        val responseData = ProjectsGet200ResponseDataDTO(pageData, page)
        return ResponseEntity.ok(ProjectsGet200ResponseDTO(data = responseData))
    }

    @Guard("create-discussion", "project")
    override fun projectsProjectIdDiscussionsPost(
        projectId: Long,
        projectsProjectIdDiscussionsPostRequestDTO: ProjectsProjectIdDiscussionsPostRequestDTO,
    ): ResponseEntity<ProjectsProjectIdDiscussionsPost200ResponseDTO> {
        // TODO: Implement
        TODO()
    }

    @Guard("query-discussion", "project")
    override fun projectsProjectIdDiscussionsGet(
        projectId: Long,
        projectFilter: ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO?,
        before: Long?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ProjectsProjectIdDiscussionsGet200ResponseDTO> {
        // TODO: Implement
        TODO()
    }

    @Guard("create-reaction", "project")
    override fun projectsProjectIdDiscussionsDiscussionIdReactionsPost(
        projectId: Long,
        discussionId: Long,
        projectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO:
            ProjectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO,
    ): ResponseEntity<ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDTO> {
        // TODO: Implement
        TODO()
    }
}
