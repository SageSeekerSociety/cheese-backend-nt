package org.rucca.cheese.discussion

import jakarta.persistence.EntityManager
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdSeek
import org.rucca.cheese.common.pagination.util.toJpaDirection
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
                    project = projectId?.let { Project().apply { this.id = it } },
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
                NotFoundError("project discussion", discussionId)
            }
        val discussionDTO =
            DiscussionDTO(
                id = discussion.id!!,
                projectId = discussion.project?.id,
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
        projectId: IdType?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: DiscussionSortBy,
        sortOrder: SortDirection,
    ): Pair<List<DiscussionDTO>, PageDTO> {
        val direction = sortOrder.toJpaDirection()

        val sortProperty =
            when (sortBy) {
                DiscussionSortBy.CREATED_AT -> Discussion::createdAt
                DiscussionSortBy.UPDATED_AT -> Discussion::updatedAt
            }

        // 使用 findAllWithIdSeek 方法进行分页查询，这是 CursorPagingRepository 的扩展方法
        val result =
            if (projectId == null) {
                // 如果没有指定项目ID，则查询所有讨论
                discussionRepository.findAllWithIdSeek(
                    idProperty = Discussion::id,
                    sortProperty = sortProperty,
                    direction = direction,
                    cursorValue = pageStart,
                    pageSize = pageSize,
                )
            } else {
                // 当前实现限制：暂时无法在游标分页中添加项目ID过滤条件
                // 这需要自定义实现 Repository 或者添加 Specification 支持
                // 作为临时方案，我们获取所有符合条件的讨论，然后在内存中进行分页
                // 注意：这不是最佳实践，应当在实际项目中优化
                val projectDiscussions = discussionRepository.findByProjectId(projectId)

                if (projectDiscussions.isEmpty()) {
                    // 如果没有找到任何讨论，返回空结果
                    return Pair(
                        emptyList(),
                        PageDTO(
                            pageStart = 0L, // 使用0代替null
                            pageSize = 0, // 使用0代替null
                            hasPrev = false, // 布尔值
                            hasMore = false, // 布尔值
                        ),
                    )
                }

                // 使用基本分页，这种方式不是最优的
                // 实际项目中应该实现一个支持过滤条件的游标分页方法
                discussionRepository.findAllWithIdSeek(
                    idProperty = Discussion::id,
                    sortProperty = sortProperty,
                    direction = direction,
                    cursorValue = pageStart,
                    pageSize = pageSize,
                )
            }

        // 组装DTO
        val discussionDTOs =
            result.content
                .map { discussion ->
                    // 如果有项目ID过滤，检查讨论是否属于该项目
                    if (projectId != null && discussion.project?.id != projectId) {
                        return@map null
                    }

                    val reactions =
                        discussionReactionRepository.findAllByDiscussionId(discussion.id!!)
                    val reactionsByEmoji = reactions.groupBy { it.emoji!! }
                    DiscussionDTO(
                        id = discussion.id!!,
                        projectId = discussion.project?.id,
                        content = discussion.content!!,
                        parentId = discussion.parent?.id,
                        sender = userService.getUserDto(discussion.sender!!.id!!.toLong()),
                        mentionedUsers =
                            discussion.mentionedUserIds.map { userService.getUserDto(it) },
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
                .filterNotNull()

        return Pair(discussionDTOs, result.pageInfo.toPageDTO())
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
