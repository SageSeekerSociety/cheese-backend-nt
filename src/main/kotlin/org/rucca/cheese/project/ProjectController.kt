package org.rucca.cheese.project

import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.discussion.DiscussionService
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(
    private val authenticationService: AuthenticationService,
    private val projectDiscussionService: DiscussionService,
) : ProjectsApi {
    @Guard("create", "project")
    override fun projectsPost(
        projectsPostRequestDTO: ProjectsPostRequestDTO
    ): ResponseEntity<ProjectsPost200ResponseDTO> {
        // TODO: Implement
        TODO()
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
        // TODO: Implement
        TODO()
    }
}
