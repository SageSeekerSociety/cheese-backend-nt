package org.rucca.cheese.project

import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(
    private val authenticationService: AuthenticationService,
    private val projectDiscussionService: ProjectDiscussionService,
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

    @Guard("create-discussion", "project")
    override fun projectsProjectIdDiscussionsPost(
        projectId: Long,
        projectsProjectIdDiscussionsPostRequestDTO: ProjectsProjectIdDiscussionsPostRequestDTO,
    ): ResponseEntity<ProjectsProjectIdDiscussionsPost200ResponseDTO> {
        val userId = authenticationService.getCurrentUserId()
        val discussionId =
            projectDiscussionService.createDiscussion(
                projectId,
                userId,
                projectsProjectIdDiscussionsPostRequestDTO.content,
                projectsProjectIdDiscussionsPostRequestDTO.parentId,
                projectsProjectIdDiscussionsPostRequestDTO.mentionedUserIds.toSet(),
            )
        val discussionDTO = projectDiscussionService.getDiscussion(discussionId)
        return ResponseEntity.ok(
            ProjectsProjectIdDiscussionsPost200ResponseDTO(
                code = 200,
                message = "OK",
                data = ProjectsProjectIdDiscussionsPost200ResponseDataDTO(discussionDTO),
            )
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
        val (discussions, page) =
            projectDiscussionService.getDiscussions(
                projectId,
                projectFilter,
                before,
                pageStart,
                pageSize,
            )
        return ResponseEntity.ok(
            ProjectsProjectIdDiscussionsGet200ResponseDTO(
                code = 200,
                message = "OK",
                data =
                    ProjectsProjectIdDiscussionsGet200ResponseDataDTO(
                        discussions = discussions,
                        page = page,
                    ),
            )
        )
    }

    @Guard("create-reaction", "project")
    override fun projectsProjectIdDiscussionsDiscussionIdReactionsPost(
        projectId: Long,
        discussionId: Long,
        projectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO:
            ProjectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO,
    ): ResponseEntity<ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDTO> {
        val userId = authenticationService.getCurrentUserId()
        val reactionDTO =
            projectDiscussionService.createReaction(
                projectId,
                discussionId,
                userId,
                projectsProjectIdDiscussionsDiscussionIdReactionsPostRequestDTO.emoji,
            )
        return ResponseEntity.ok(
            ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDTO(
                code = 200,
                message = "OK",
                data =
                    ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataDTO(
                        reaction = reactionDTO
                    ),
            )
        )
    }
}
