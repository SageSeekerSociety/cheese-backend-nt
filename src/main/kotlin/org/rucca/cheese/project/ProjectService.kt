package org.rucca.cheese.project

import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.ProjectDTO
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
    fun createProject(requestDTO: ProjectsPostRequestDTO): ProjectDTO {
        var project =
            Project(
                name = requestDTO.name,
                description = requestDTO.description,
                colorCode = requestDTO.colorCode,
                startDate = requestDTO.startDate.toLocalDateTime(),
                endDate = requestDTO.endDate.toLocalDateTime(),
                // TODO: 团队ID待修改
                // team = requestDTO.teamId?.let { teamRepository.findById(it).orElse(null)
                // },
                team = null,
                leader = requestDTO.leaderId.toInt().let { userRepository.findById(it).orElse(null) },
                parent = requestDTO.parentId?.let { projectRepository.findById(it).orElse(null) },
                externalTaskId = requestDTO.externalTaskId,
                githubRepo = requestDTO.githubRepo,
            )

        project = projectRepository.save(project)
        val projectDTO = convertProjectToDTO(project)
        return projectDTO;
    }

    fun getProjects(
        parentId: Long?,
        leaderId: Long?,
        memberId: Long?,
        status: String?,
        pageStart: Long?,
        pageSize: Int,
    ): Pair<List<ProjectDTO>, PageDTO> {
        // Implement the query logic here
        // For example, using a combination of repository methods or a custom query
        val projects = projectRepository.findAll()
        val projectDTOs =
            projects.map { project -> convertProjectToDTO(project) }
        val (pageData, page) =
            PageHelper.pageFromAll(
                projectDTOs,
                pageStart,
                pageSize,
                { it.id },
                { throw NotFoundError("project", it) },
            )
        return Pair(pageData, page)
    }

    private fun convertProjectToDTO(project: Project): ProjectDTO {
        return ProjectDTO(
            id = project.id!!,
            name = project.name!!,
            description = project.description!!,
            startDate = project.startDate!!.toEpochMilli(),
            endDate = project.endDate!!.toEpochMilli(),
            leaderId = project.leader!!.id!!.toLong(),
            content = project.content!!,
            colorCode = project.colorCode,
            parentId = project.parent?.id,
            externalTaskId = project.externalTaskId,
            githubRepo = project.githubRepo,
            // path = project.path,
            createdAt = project.createdAt!!.toEpochMilli(),
            updatedAt = project.updatedAt!!.toEpochMilli(),
        )
    }
}
