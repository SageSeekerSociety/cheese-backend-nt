package org.rucca.cheese.discussion

import org.hibernate.query.SortDirection
import org.rucca.cheese.api.DiscussionsApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.spring.*
import org.rucca.cheese.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseNewAuth
class DiscussionController(
    private val jwtService: JwtService,
    private val discussionService: DiscussionService,
    private val reactionService: DiscussionReactionService,
) : DiscussionsApi {
    @Auth("discussion:list:discussion")
    override fun listDiscussions(
        @AuthUser userInfo: AuthUserInfo?,
        @AuthContext("modelType") modelType: DiscussableModelTypeDTO?,
        @AuthContext("modelId") modelId: Long?,
        @AuthContext("parentId") parentId: Long?,
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
                userInfo?.userId,
            )
        return ResponseEntity.ok(
            ListDiscussions200ResponseDTO(
                code = 200,
                message = "OK",
                data = ListDiscussions200ResponseDataDTO(discussions = discussions, page = page),
            )
        )
    }

    @Auth("discussion:create:discussion")
    override fun createDiscussion(
        @AuthContext("modelType", field = "modelType")
        @AuthContext("modelId", field = "modelId")
        @AuthContext("parentId", field = "parentId")
        createDiscussionRequestDTO: CreateDiscussionRequestDTO
    ): ResponseEntity<CreateDiscussion200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val modelTypeEnum = DiscussableModelType.valueOf(createDiscussionRequestDTO.modelType.name)
        val discussionDTO =
            discussionService.createDiscussion(
                userId,
                createDiscussionRequestDTO.content,
                createDiscussionRequestDTO.parentId,
                createDiscussionRequestDTO.mentionedUserIds?.toSet() ?: setOf(),
                modelTypeEnum,
                createDiscussionRequestDTO.modelId,
            )
        return ResponseEntity.ok(
            CreateDiscussion200ResponseDTO(
                code = 200,
                message = "OK",
                data = CreateDiscussion200ResponseDataDTO(discussionDTO),
            )
        )
    }

    @Auth("discussion:view:discussion")
    override fun getDiscussion(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId discussionId: Long,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<GetDiscussion200ResponseDTO> {
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

        val discussionDTO = discussionService.getDiscussion(discussionId, userInfo?.userId)
        val (subDiscussions, page) =
            discussionService.getDiscussions(
                modelType = null,
                modelId = null,
                parentId = discussionId,
                pageStart = pageStart,
                pageSize = pageSize,
                sortBy = by,
                sortOrder = order,
                currentUserId = userInfo?.userId,
            )
        return ResponseEntity.ok(
            GetDiscussion200ResponseDTO(
                code = 200,
                message = "OK",
                data =
                    GetDiscussion200ResponseDataDTO(
                        discussion = discussionDTO,
                        subDiscussions =
                            GetDiscussion200ResponseDataSubDiscussionsDTO(
                                discussions = subDiscussions,
                                page = page,
                            ),
                    ),
            )
        )
    }

    @Auth("discussion:view:discussion")
    override fun listSubDiscussions(
        @AuthUser userInfo: AuthUserInfo?,
        @ResourceId discussionId: Long,
        pageStart: Long?,
        pageSize: Int,
        sortBy: String,
        sortOrder: String,
    ): ResponseEntity<ListSubDiscussions200ResponseDTO> {
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

        val (subDiscussions, page) =
            discussionService.getDiscussions(
                modelType = null,
                modelId = null,
                parentId = discussionId,
                pageStart = pageStart,
                pageSize = pageSize,
                sortBy = by,
                sortOrder = order,
                currentUserId = userInfo?.userId,
            )
        return ResponseEntity.ok(
            ListSubDiscussions200ResponseDTO(
                code = 200,
                message = "OK",
                data = ListDiscussions200ResponseDataDTO(discussions = subDiscussions, page = page),
            )
        )
    }

    @Auth("discussion:update:discussion")
    override fun patchDiscussion(
        discussionId: Long,
        patchDiscussionRequestDTO: PatchDiscussionRequestDTO,
    ): ResponseEntity<PatchDiscussion200ResponseDTO> {
        val discussionDTO =
            discussionService.updateDiscussion(discussionId, patchDiscussionRequestDTO)
        return ResponseEntity.ok(
            PatchDiscussion200ResponseDTO(
                code = 200,
                message = "OK",
                data = CreateDiscussion200ResponseDataDTO(discussionDTO),
            )
        )
    }

    @Auth("discussion:delete:discussion")
    override fun deleteDiscussion(@ResourceId discussionId: Long): ResponseEntity<Unit> {
        discussionService.deleteDiscussion(discussionId)
        return ResponseEntity.noContent().build()
    }

    @Auth("discussion:react:reaction")
    override fun reactToDiscussion(
        @ResourceId discussionId: Long,
        reactionTypeId: Long,
    ): ResponseEntity<ReactToDiscussion200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val reactionDTO = reactionService.toggleReaction(discussionId, userId, reactionTypeId)
        return ResponseEntity.ok(
            ReactToDiscussion200ResponseDTO(
                code = 200,
                message = "OK",
                data = ReactToDiscussion200ResponseDataDTO(reaction = reactionDTO),
            )
        )
    }

    @SkipSecurity
    override fun getAllReactionTypes(): ResponseEntity<GetAllReactionTypes200ResponseDTO> {
        val reactionTypes = reactionService.getAllReactionTypes()
        return ResponseEntity.ok(
            GetAllReactionTypes200ResponseDTO(
                code = 200,
                message = "OK",
                data = GetAllReactionTypes200ResponseAllOfDataDTO(reactionTypes),
            )
        )
    }
}
