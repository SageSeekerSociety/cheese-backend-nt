package org.rucca.cheese.project

import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.model.ProjectsPostRequestDTO
import org.rucca.cheese.team.TeamRepository
import org.rucca.cheese.user.UserRepository
import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val projectDiscussionRepository: ProjectDiscussionRepository,
    private val projectDiscussionReactionRepository: ProjectDiscussionReactionRepository,
    private val projectExternalCollaboratorRepository: ProjectExternalCollaboratorRepository,
) {
    fun createProject(requestDTO: ProjectsPostRequestDTO): Project {
        val project =
            Project(
                name = requestDTO.name,
                description = requestDTO.description,
                colorCode = requestDTO.colorCode,
                startDate = requestDTO.startDate.toLocalDateTime(),
                endDate = requestDTO.endDate.toLocalDateTime(),
                // TODO: 团队ID待修改
                // team = requestDTO.teamId?.let { teamRepository.findById(it).orElse(null) },
                team = null,
                leader =
                    requestDTO.leaderId.toInt().let { userRepository.findById(it).orElse(null) },
                parent = requestDTO.parentId?.let { projectRepository.findById(it).orElse(null) },
                externalTaskId = requestDTO.externalTaskId,
                githubRepo = requestDTO.githubRepo,
            )
        return projectRepository.save(project)
    }

    fun getProjects(
        parentId: Long?,
        leaderId: Long?,
        memberId: Long?,
        status: String?,
        pageStart: Long?,
        pageSize: Int,
    ): List<Project> {
        // Implement the query logic here
        // For example, using a combination of repository methods or a custom query
        return projectRepository.findAll()
    }
}
