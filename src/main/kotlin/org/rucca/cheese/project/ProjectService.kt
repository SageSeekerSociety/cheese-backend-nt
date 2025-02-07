package org.rucca.cheese.project

import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectDiscussionRepository: ProjectDiscussionRepository,
    private val projectDiscussionReactionRepository: ProjectDiscussionReactionRepository,
    private val projectExternalCollaboratorRepository: ProjectExternalCollaboratorRepository,
) {
    // TODO: Implement
}
