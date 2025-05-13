/*
 *  Description: This file defines the Discussion service.
 *               It contains the business logic for discussions.
 */

package org.rucca.cheese.discussion

import java.time.LocalDateTime
import java.time.ZoneId
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdCursor
import org.rucca.cheese.common.pagination.repository.idSeekSpec
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.getProperty
import org.rucca.cheese.knowledge.KnowledgeService
import org.rucca.cheese.model.*
import org.rucca.cheese.user.User
import org.rucca.cheese.user.services.UserService
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscussionService(
    private val discussionRepository: DiscussionRepository,
    private val discussionReactionService: DiscussionReactionService,
    private val userService: UserService,
    private val entityPatcher: EntityPatcher,
    private val applicationContext: ApplicationContext,
    //    private val userRepository: UserRepository,
) {
    @Cacheable("discussableModelTypeAndId", key = "#discussionId")
    fun getModelTypeAndIdFromDiscussionId(
        discussionId: IdType
    ): Pair<DiscussableModelTypeDTO, IdType> {
        val discussion =
            discussionRepository.findModelTypeAndIdById(discussionId)
                ?: throw NotFoundError("discussion", discussionId)
        return Pair(discussion.modelType.toDTO(), discussion.modelId)
    }

    @Transactional
    fun deleteDiscussion(discussionId: IdType) {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("discussion", discussionId)
            }
        discussion.deletedAt = LocalDateTime.now()
        discussionRepository.save(discussion)
    }

    @Transactional
    fun createDiscussion(
        senderId: IdType,
        content: String,
        parentId: IdType?,
        mentionedUserIds: Set<IdType>,
        modelType: DiscussableModelType,
        modelId: IdType,
    ): DiscussionDTO {
        val discussion =
            discussionRepository.save(
                Discussion(
                    modelType = modelType,
                    modelId = modelId,
                    sender = User().apply { this.id = senderId.toInt() },
                    content = content,
                    parent = parentId?.let { discussionRepository.getReferenceById(it) },
                    mentionedUserIds = mentionedUserIds,
                )
            )
        return discussion.toDTO()
    }

    private fun Discussion.toDTO(
        currentUserId: IdType? = null,
        withReactions: Boolean = true,
        withSubDiscussions: Boolean = true,
        replyPageSize: Int = 2,
    ): DiscussionDTO {
        val reactions =
            if (withReactions)
                discussionReactionService.getReactionSummaries(this.id!!, currentUserId)
            else emptyList()
        val (subDiscussions, _) =
            if (withSubDiscussions)
                getDiscussions(
                    modelType = this.modelType,
                    modelId = this.modelId,
                    parentId = this.id,
                    pageStart = null,
                    pageSize = replyPageSize,
                    sortBy = DiscussionSortBy.CREATED_AT,
                    sortOrder = SortDirection.DESCENDING,
                    currentUserId = currentUserId,
                    withReactions = withReactions,
                    withSubDiscussions = false,
                )
            else Pair(null, null)
        val subDiscussionCount =
            if (withSubDiscussions) discussionRepository.countByParentId(this.id!!) else null
        return DiscussionDTO(
            id = this.id!!,
            modelType = DiscussableModelTypeDTO.valueOf(this.modelType.name),
            modelId = this.modelId,
            content = this.content,
            parentId = this.parent?.id,
            sender = userService.getUserDto(this.sender.id!!.toLong()),
            mentionedUsers = this.mentionedUserIds.map { userService.getUserDto(it) },
            reactions = reactions,
            subDiscussions =
                DiscussionSubDiscussionsDTO(count = subDiscussionCount, examples = subDiscussions)
                    .takeIf { withSubDiscussions },
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }

    fun getDiscussion(
        discussionId: IdType,
        currentUserId: IdType? = null,
        withReactions: Boolean = true,
        withSubDiscussions: Boolean = false,
    ): DiscussionDTO {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("discussion", discussionId)
            }
        return discussion.toDTO(currentUserId, withReactions, withSubDiscussions)
    }

    fun updateDiscussion(discussionId: IdType, patch: PatchDiscussionRequestDTO): DiscussionDTO {
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("discussion", discussionId)
            }
        entityPatcher.patch(discussion, patch)
        return discussionRepository.save(discussion).toDTO()
    }

    enum class DiscussionSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    fun getDiscussions(
        modelType: DiscussableModelType?,
        modelId: IdType?,
        parentId: IdType?,
        pageStart: Long?,
        pageSize: Int,
        sortBy: DiscussionSortBy,
        sortOrder: SortDirection,
        currentUserId: IdType? = null,
        withReactions: Boolean = true,
        withSubDiscussions: Boolean = true,
    ): Pair<List<DiscussionDTO>, PageDTO> {
        val direction = sortOrder.toJpaDirection()

        val spec =
            Specification.where<Discussion>(null)
                .and(
                    modelType?.let {
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
                    if (parentId == null)
                        Specification.where { root, _, cb ->
                            cb.isNull(root.getProperty(Discussion::parent))
                        }
                    else
                        Specification.where { root, _, cb ->
                            cb.equal(
                                root.getProperty(Discussion::parent).getProperty(Discussion::id),
                                parentId,
                            )
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
            result.content.map { it.toDTO(currentUserId, withReactions, withSubDiscussions) }

        return Pair(discussionDTOs, result.pageInfo.toPageDTO())
    }

    /** 检查用户是否为讨论的创建者 */
    fun isDiscussionCreator(discussionId: IdType, userId: IdType): Boolean {
        val discussion = discussionRepository.findById(discussionId).orElse(null) ?: return false
        return discussion.sender.id?.toLong() == userId
    }

    /** 检查用户是否在讨论中被提及 */
    fun isUserMentioned(discussionId: IdType, userId: IdType): Boolean {
        val discussion = discussionRepository.findById(discussionId).orElse(null) ?: return false
        return discussion.mentionedUserIds.contains(userId)
    }

    /**
     * 从讨论创建知识条目
     *
     * @param discussionId 讨论ID
     * @param name 知识条目名称
     * @param description 知识条目描述
     * @param teamId 知识条目所属团队ID
     * @param labels 标签列表
     * @param userId 操作用户ID
     * @return 新创建的知识条目ID
     */
    @Transactional
    fun saveDiscussionToKnowledge(
        discussionId: Long,
        name: String,
        description: String,
        teamId: Long,
        labels: List<String>? = null,
        userId: Long,
    ): Long {
        // 获取讨论实体
        val discussion =
            discussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("discussion", discussionId)
            }

        val knowledgeService = applicationContext.getBean(KnowledgeService::class.java)

        // 根据讨论类型确定知识条目类型
        val knowledgeType = KnowledgeTypeDTO.TEXT

        // 创建知识条目
        val knowledgeDTO =
            knowledgeService.createKnowledge(
                name = name,
                type = knowledgeType,
                userId = userId,
                content = discussion.content,
                description = description,
                teamId = teamId,
                projectId =
                    if (discussion.modelType == DiscussableModelType.PROJECT) discussion.modelId
                    else null,
                labels = labels,
                discussionId = discussionId,
            )

        // 通知讨论的创建者
        //        val creatorId = discussion.sender.id!!
        //        if (creatorId != userId.toInt()) {
        //            notificationService.createNotification(
        //                receiverId = creatorId.toLong(),
        //                type = NotificationType.DISCUSSION_SAVED_TO_KNOWLEDGE,
        //                data =
        //                    mapOf(
        //                        "discussionId" to discussionId,
        //                        "knowledgeId" to knowledgeDTO.id,
        //                        "knowledgeName" to knowledgeDTO.name,
        //                        "savedByUserId" to userId,
        //                    ),
        //            )
        //        }

        return knowledgeDTO.id
    }
}
