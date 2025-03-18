package org.rucca.cheese.discussion

import org.hibernate.query.SortDirection
import org.rucca.cheese.api.DiscussionsApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.spring.UseOldAuth
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseOldAuth
class DiscussionController(
    private val jwtService: JwtService,
    private val discussionService: DiscussionService,
) : DiscussionsApi {
    @Guard("query-discussion", "project")
    override fun listDiscussions(
        modelType: DiscussableModelTypeDTO?,
        modelId: Long?,
        parentId: Long?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<ListDiscussions200ResponseDTO> {
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
        val modelTypeEnum = modelType?.let { DiscussableModelType.valueOf(modelType.name) }
        val (discussions, page) =
            discussionService.getDiscussions(
                modelTypeEnum,
                modelId,
                parentId,
                pageStart,
                pageSize,
                by,
                order,
            )
        return ResponseEntity.ok(
            ListDiscussions200ResponseDTO(
                code = 200,
                message = "OK",
                data = ListDiscussions200ResponseDataDTO(discussions = discussions, page = page),
            )
        )
    }

    @Guard("create-discussion", "project")
    override fun createDiscussion(
        createDiscussionRequestDTO: CreateDiscussionRequestDTO
    ): ResponseEntity<CreateDiscussion200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val modelTypeEnum = DiscussableModelType.valueOf(createDiscussionRequestDTO.modelType.name)
        val discussionId =
            discussionService.createDiscussion(
                userId,
                createDiscussionRequestDTO.content,
                createDiscussionRequestDTO.parentId,
                createDiscussionRequestDTO.mentionedUserIds?.toSet() ?: setOf(),
                modelTypeEnum,
                createDiscussionRequestDTO.modelId,
            )
        val discussionDTO = discussionService.getDiscussion(discussionId)
        return ResponseEntity.ok(
            CreateDiscussion200ResponseDTO(
                code = 200,
                message = "OK",
                data = CreateDiscussion200ResponseDataDTO(discussionDTO),
            )
        )
    }

    @Guard("create-reaction", "project")
    override fun reactToDiscussion(
        discussionId: Long,
        reactToDiscussionRequestDTO: ReactToDiscussionRequestDTO,
    ): ResponseEntity<ReactToDiscussion200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val reactionDTO =
            discussionService.createReaction(
                discussionId,
                userId,
                reactToDiscussionRequestDTO.emoji,
            )
        return ResponseEntity.ok(
            ReactToDiscussion200ResponseDTO(
                code = 200,
                message = "OK",
                data = ReactToDiscussion200ResponseDataDTO(reaction = reactionDTO),
            )
        )
    }
}
