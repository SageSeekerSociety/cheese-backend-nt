package org.rucca.cheese.discussion

import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.project.Project
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
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
        projectId: IdType?,
    ): IdType {
        val discussion =
            discussionRepository.save(
                Discussion(
                    project = Project().apply { this.id = projectId },
                    sender = User().apply { this.id = senderId.toInt() },
                    content = content,
                    parent = Discussion().apply { this.id = parentId },
                    mentionedUserIds = mentionedUserIds,
                )
            )
        return discussion.id!!
    }

    fun getDiscussion(discussionId: IdType): DiscussionDTO {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("project discussion", discussionId)
            }
        val discussionDTO =
            DiscussionDTO(
                id = discussion.id!!,
                projectId = discussion.project!!.id!!,
                content = discussion.content!!,
                parentId = discussion.parent!!.id!!,
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
        projectId: IdType?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: DiscussionSortBy,
        sortOrder: SortDirection,
    ): Pair<List<DiscussionDTO>, PageDTO> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val cq = criteriaBuilder.createQuery(Discussion::class.java)
        val root = cq.from(Discussion::class.java)
        val by =
            when (sortBy) {
                DiscussionSortBy.CREATED_AT -> root.get<LocalDateTime>("createdAt")
                DiscussionSortBy.UPDATED_AT -> root.get<LocalDateTime>("updatedAt")
            }
        val order =
            when (sortOrder) {
                SortDirection.ASCENDING -> criteriaBuilder.asc(by)
                SortDirection.DESCENDING -> criteriaBuilder.desc(by)
            }
        cq.orderBy(order)
        val query = entityManager.createQuery(cq)
        val result = query.resultList

        val (curr, page) =
            PageHelper.pageFromAll(
                result,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("space", id) },
            )

        val discussionDTOs =
            curr.map { discussion ->
                val reactions =
                    discussionReactionRepository.findAllByProjectDiscussionId(discussion.id!!)
                val reactionsByEmoji = reactions.groupBy { it.emoji!! }
                DiscussionDTO(
                    id = discussion.id!!,
                    projectId = discussion.project!!.id!!,
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
        return Pair(discussionDTOs, page)
    }

    fun createReaction(
        discussionId: IdType,
        userId: IdType,
        emoji: String,
    ): DiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("project discussion", discussionId)
            }

        val reaction =
            DiscussionReaction(
                projectDiscussion = discussion,
                user = User().apply { this.id = userId.toInt() },
                emoji = emoji,
            )
        discussionReactionRepository.save(reaction)

        val reactions = discussionReactionRepository.findAllByProjectDiscussionId(discussionId)
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
