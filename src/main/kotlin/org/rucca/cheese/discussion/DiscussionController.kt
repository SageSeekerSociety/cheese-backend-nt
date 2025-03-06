package org.rucca.cheese.discussion

import org.hibernate.query.SortDirection
import org.rucca.cheese.api.DiscussionsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DiscussionController(
    private val authenticationService: AuthenticationService,
    private val discussionService: DiscussionService,
) : DiscussionsApi {
    @Guard("query-discussion", "project")
    override fun discussionsGet(
        projectId: Long?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<DiscussionsGet200ResponseDTO> {
        val by =
            when (sortBy) {
                "updatedAt" -> DiscussionService.DiscussionSortBy.UPDATED_AT
                "createdAt" -> DiscussionService.DiscussionSortBy.CREATED_AT
                else -> throw IllegalArgumentException("Invalid sortBy: $sortBy")
            }
        val order =
            when (sortOrder) {
                "asc" -> SortDirection.ASCENDING
                "desc" -> SortDirection.DESCENDING
                else -> throw IllegalArgumentException("Invalid sortOrder: $sortOrder")
            }
        val (discussions, page) =
            discussionService.getDiscussions(projectId, pageStart, pageSize, by, order)
        return ResponseEntity.ok(
            DiscussionsGet200ResponseDTO(
                code = 200,
                message = "OK",
                data = DiscussionsGet200ResponseDataDTO(discussions = discussions, page = page),
            )
        )
    }

    @Guard("create-discussion", "project")
    override fun discussionsPost(
        discussionsPostRequestDTO: DiscussionsPostRequestDTO
    ): ResponseEntity<DiscussionsPost200ResponseDTO> {
        val userId = authenticationService.getCurrentUserId()
        val discussionId =
            discussionService.createDiscussion(
                userId,
                discussionsPostRequestDTO.content,
                discussionsPostRequestDTO.parentId,
                discussionsPostRequestDTO.mentionedUserIds?.toSet() ?: setOf(),
                discussionsPostRequestDTO.projectId,
            )
        val discussionDTO = discussionService.getDiscussion(discussionId)
        return ResponseEntity.ok(
            DiscussionsPost200ResponseDTO(
                code = 200,
                message = "OK",
                data = DiscussionsPost200ResponseDataDTO(discussionDTO),
            )
        )
    }

    @Guard("create-reaction", "project")
    override fun discussionsDiscussionIdReactionsPost(
        discussionId: Long,
        discussionsDiscussionIdReactionsPostRequestDTO:
            DiscussionsDiscussionIdReactionsPostRequestDTO,
    ): ResponseEntity<DiscussionsDiscussionIdReactionsPost200ResponseDTO> {
        val userId = authenticationService.getCurrentUserId()
        val reactionDTO =
            discussionService.createReaction(
                discussionId,
                userId,
                discussionsDiscussionIdReactionsPostRequestDTO.emoji,
            )
        return ResponseEntity.ok(
            DiscussionsDiscussionIdReactionsPost200ResponseDTO(
                code = 200,
                message = "OK",
                data =
                    DiscussionsDiscussionIdReactionsPost200ResponseDataDTO(reaction = reactionDTO),
            )
        )
    }
}
