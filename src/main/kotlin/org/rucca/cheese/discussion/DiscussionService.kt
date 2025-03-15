package org.rucca.cheese.discussion

import jakarta.persistence.EntityManager
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdCursor
import org.rucca.cheese.common.pagination.repository.idSeekSpec
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

@Service
class DiscussionService(
    private val discussionRepository: DiscussionRepository,
    private val discussionReactionRepository: DiscussionReactionRepository,
    private val userService: UserService,
    private val entityManager: EntityManager,
) {
    fun createDiscussion(
        senderId: IdType,
        content: String,
        parentId: IdType?,
        mentionedUserIds: Set<IdType>,
        modelType: DiscussableModelType,
        modelId: IdType,
    ): IdType {
        val discussion =
            discussionRepository.save(
                Discussion(
                    modelType = modelType,
                    modelId = modelId,
                    sender = User().apply { this.id = senderId.toInt() },
                    content = content,
                    parent = parentId?.let { Discussion().apply { this.id = it } },
                    mentionedUserIds = mentionedUserIds,
                )
            )
        return discussion.id!!
    }

    fun getDiscussion(discussionId: IdType): DiscussionDTO {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("discussion", discussionId)
            }
        val discussionDTO =
            DiscussionDTO(
                id = discussion.id!!,
                modelType = DiscussableModelTypeDTO.valueOf(discussion.modelType!!.name),
                modelId = discussion.modelId!!,
                content = discussion.content!!,
                parentId = discussion.parent?.id,
                sender = userService.getUserDto(discussion.sender!!.id!!.toLong()),
                mentionedUsers = discussion.mentionedUserIds.map { userService.getUserDto(it) },
                reactions = emptyList(),
                createdAt = discussion.createdAt!!.toEpochMilli(),
            )
        return discussionDTO
    }

    enum class DiscussionSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    fun getDiscussions(
        discussableModelType: DiscussableModelType?,
        modelId: IdType?,
        parentId: IdType?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: DiscussionSortBy,
        sortOrder: SortDirection,
    ): Pair<List<DiscussionDTO>, PageDTO> {
        val direction = sortOrder.toJpaDirection()

        val spec =
            Specification.where<Discussion>(null)
                .and(
                    discussableModelType?.let {
                        Specification.where { root, _, cb ->
                            cb.equal(root.get<DiscussableModelType>("modelType"), it)
                        }
                    }
                )
                .and(
                    modelId?.let {
                        Specification.where { root, _, cb ->
                            cb.equal(root.get<IdType>("modelId"), it)
                        }
                    }
                )
                .and(
                    parentId?.let {
                        Specification.where { root, _, cb ->
                            cb.equal(root.get<Discussion>("parent").get<IdType>("id"), it)
                        }
                    }
                )

        val sortProperty =
            when (sortBy) {
                DiscussionSortBy.CREATED_AT -> Discussion::createdAt
                DiscussionSortBy.UPDATED_AT -> Discussion::updatedAt
            }

        val cursorSpec =
            discussionRepository
                .idSeekSpec(Discussion::id, sortProperty = sortProperty, direction = direction)
                .specification(spec)
                .build()

        val result = discussionRepository.findAllWithIdCursor(cursorSpec, pageStart, pageSize)

        // 组装DTO并添加反应
        val discussionDTOs =
            result.content.map { discussion ->
                val reactions = discussionReactionRepository.findAllByDiscussionId(discussion.id!!)
                val reactionsByEmoji = reactions.groupBy { it.emoji!! }

                DiscussionDTO(
                    id = discussion.id!!,
                    modelType = DiscussableModelTypeDTO.valueOf(discussion.modelType!!.name),
                    modelId = discussion.modelId!!,
                    content = discussion.content!!,
                    parentId = discussion.parent?.id,
                    sender = userService.getUserDto(discussion.sender!!.id!!.toLong()),
                    mentionedUsers = discussion.mentionedUserIds.map { userService.getUserDto(it) },
                    reactions =
                        reactionsByEmoji.map { (emoji, reactionList) ->
                            DiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
                                emoji = emoji,
                                count = reactionList.size,
                                users =
                                    reactionList.map {
                                        userService.getUserDto(it.user!!.id!!.toLong())
                                    },
                            )
                        },
                    createdAt = discussion.createdAt!!.toEpochMilli(),
                )
            }

        return Pair(discussionDTOs, result.pageInfo.toPageDTO())
    }

    fun createReaction(
        discussionId: IdType,
        userId: IdType,
        emoji: String,
    ): DiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("discussion", discussionId)
            }

        val reaction =
            DiscussionReaction(
                discussion = discussion,
                user = User().apply { this.id = userId.toInt() },
                emoji = emoji,
            )
        discussionReactionRepository.save(reaction)

        val reactions = discussionReactionRepository.findAllByDiscussionId(discussionId)
        val reactionsByEmoji = reactions.groupBy { it.emoji!! }
        val reactionDTO =
            reactionsByEmoji[emoji]?.let { reactionList ->
                DiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
                    emoji = emoji,
                    count = reactionList.size,
                    users = reactionList.map { userService.getUserDto(it.user!!.id!!.toLong()) },
                )
            }
                ?: DiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
                    emoji = emoji,
                    count = 0,
                    users = emptyList(),
                )

        return reactionDTO
    }
}
