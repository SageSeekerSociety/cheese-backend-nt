package org.rucca.cheese.project

import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController : ProjectsApi {
    @Guard("create", "project")
    override fun projectsPost(
        projectsPostRequestDTO: ProjectsPostRequestDTO
    ): ResponseEntity<ProjectsPost200ResponseDTO> {
        return super.projectsPost(projectsPostRequestDTO)
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
        return super.projectsGet(parentId, leaderId, memberId, status, pageStart, pageSize)
    }

    @Guard("create-discussion", "project")
    override fun projectsProjectIdDiscussionsPost(
        projectId: Long,
        projectsProjectIdDiscussionsPostRequestDTO: ProjectsProjectIdDiscussionsPostRequestDTO,
    ): ResponseEntity<ProjectsProjectIdDiscussionsPost200ResponseDTO> {
        return super.projectsProjectIdDiscussionsPost(
            projectId,
            projectsProjectIdDiscussionsPostRequestDTO,
        )
    }

    @Guard("query-discussion", "project")
    override fun projectsProjectIdDiscussionsGet(
        projectId: Long,
        projectFilter: ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO?,
        before: Long?,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<ProjectsProjectIdDiscussionsGet200ResponseDTO> {
        return super.projectsProjectIdDiscussionsGet(
            projectId,
            projectFilter,
            before,
            pageStart,
            pageSize,
        )
    }

    @Guard("create-reaction", "project")
    override fun projectsProjectIdDiscussionsDiscussionIdReactionsPost(
        projectId: Long,
        discussionId: Long,
        projectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO:
            ProjectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO,
    ): ResponseEntity<ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDTO> {
        return super.projectsProjectIdDiscussionsDiscussionIdReactionsPost(
            projectId,
            discussionId,
            projectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO,
        )
    }
}
