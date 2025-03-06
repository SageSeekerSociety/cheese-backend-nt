package org.rucca.cheese.project

import org.rucca.cheese.discussion.DiscussionReactionRepository
import org.rucca.cheese.discussion.DiscussionRepository
import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectDiscussionRepository: DiscussionRepository,
    private val projectDiscussionReactionRepository: DiscussionReactionRepository,
    private val projectExternalCollaboratorRepository: ProjectExternalCollaboratorRepository,
) {
    // TODO: Implement
}
