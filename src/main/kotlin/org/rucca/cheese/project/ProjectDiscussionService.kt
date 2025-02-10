package org.rucca.cheese.project

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.DiscussionDTO
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
}
