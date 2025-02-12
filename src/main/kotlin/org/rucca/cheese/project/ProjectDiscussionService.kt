package org.rucca.cheese.project

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service

@Service
class ProjectDiscussionService(
    private val projectRepository: ProjectRepository,
    private val projectDiscussionRepository: ProjectDiscussionRepository,
    private val projectDiscussionReactionRepository: ProjectDiscussionReactionRepository,
    private val projectExternalCollaboratorRepository: ProjectExternalCollaboratorRepository,
    private val userService: UserService,
) {
    fun createDiscussion(
        projectId: IdType,
        senderId: IdType,
        content: String,
        parentId: IdType?,
        mentionedUserIds: Set<IdType>,
    ): IdType {
        val discussion =
            projectDiscussionRepository.save(
                ProjectDiscussion(
                    project = Project().apply { this.id = projectId },
                    sender = User().apply { this.id = senderId.toInt() },
                    content = content,
                    parent = ProjectDiscussion().apply { this.id = parentId },
                    mentionedUserIds = mentionedUserIds,
                )
            )
        return discussion.id!!
    }

    fun getDiscussion(discussionId: IdType): DiscussionDTO {
        val discussion =
            projectDiscussionRepository.findById(discussionId).orElseThrow {
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

    fun getDiscussions(
        projectId: IdType,
        projectFilter: ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO?,
        before: Long?,
        pageStart: Long?,
        pageSize: Int,
    ): Pair<List<DiscussionDTO>, PageDTO> {
        val projectIds =
            when (projectFilter?.type) {
                ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO.Type.projects ->
                    projectFilter.projectIds?.toList() ?: listOf(projectId)
                ProjectsProjectIdDiscussionsGetProjectFilterParameterDTO.Type.tree -> {
                    val rootId = projectFilter.rootProjectId ?: projectId
                    // TODO: 实现获取项目树中所有项目ID的逻辑
                    listOf(rootId)
                }
                null -> listOf(projectId)
            }

        val discussions = projectDiscussionRepository.findAllByProjectIdIn(projectIds)

        val (curr, page) =
            PageHelper.pageFromAll(
                discussions,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("space", id) },
            )

        val discussionDTOs =
            curr.map { discussion ->
                val reactions =
                    projectDiscussionReactionRepository.findAllByProjectDiscussionId(
                        discussion.id!!
                    )
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
                            ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
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
        projectId: IdType,
        discussionId: IdType,
        userId: IdType,
        emoji: String,
    ): ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO {
        val discussion =
            projectDiscussionRepository.findById(discussionId).orElseThrow {
                NotFoundError("project discussion", discussionId)
            }

        if (discussion.project?.id != projectId) {
            throw NotFoundError("project discussion", discussionId)
        }

        val reaction =
            ProjectDiscussionReaction(
                projectDiscussion = discussion,
                user = User().apply { this.id = userId.toInt() },
                emoji = emoji,
            )
        projectDiscussionReactionRepository.save(reaction)

        val reactions =
            projectDiscussionReactionRepository.findAllByProjectDiscussionId(discussionId)
        val reactionsByEmoji = reactions.groupBy { it.emoji!! }
        val reactionDTO =
            reactionsByEmoji[emoji]?.let { reactionList ->
                ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
                    emoji = emoji,
                    count = reactionList.size,
                    users = reactionList.map { userService.getUserDto(it.user!!.id!!.toLong()) },
                )
            }
                ?: ProjectsProjectIdDiscussionsDiscussionIdReactionsPost200ResponseDataReactionDTO(
                    emoji = emoji,
                    count = 0,
                    users = emptyList(),
                )

        return reactionDTO
    }
}
