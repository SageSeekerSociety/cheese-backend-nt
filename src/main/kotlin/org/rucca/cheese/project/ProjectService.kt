package org.rucca.cheese.project

import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdCursor
import org.rucca.cheese.common.pagination.repository.idSeekSpec
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.ProjectDTO
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val teamService: TeamService,
    private val userService: UserService,
) {

    @Transactional
    fun createProject(
        name: String,
        description: String,
        colorCode: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        teamId: IdType,
        leaderId: IdType,
        content: String,
        parentId: IdType? = null,
        externalTaskId: IdType? = null,
        githubRepo: String? = null,
    ): IdType {
        teamService.ensureTeamIdExists(teamId)
        userService.ensureUserIdExists(leaderId)
        val parentProject = parentId?.let { projectRepository.findById(it).orElse(null) }
        val project =
            Project(
                name = name,
                description = description,
                colorCode = colorCode,
                startDate = startDate,
                endDate = endDate,
                team = Team().apply { id = teamId },
                leader = User().apply { id = leaderId.toInt() },
                content = content,
                parent = parentProject,
                externalTaskId = externalTaskId,
                githubRepo = githubRepo,
            )
        return projectRepository.save(project).id!!
    }

    fun getProjectDto(projectId: IdType): ProjectDTO {
        val project =
            projectRepository.findById(projectId).orElseThrow {
                NotFoundError("project", projectId)
            }
        return project.toProjectDTO()
    }

    fun Project.toProjectDTO(): ProjectDTO {
        return ProjectDTO(
            id = this.id!!,
            name = this.name!!,
            description = this.description!!,
            colorCode = this.colorCode!!,
            startDate = this.startDate!!.toEpochMilli(),
            endDate = this.endDate!!.toEpochMilli(),
            leader = userService.getUserDto(this.leader!!.id!!.toLong()),
            team = teamService.getTeamDto(this.team!!.id!!.toLong()),
            content = this.content!!,
            parentId = this.parent?.id,
            externalTaskId = this.externalTaskId,
            githubRepo = this.githubRepo,
            createdAt = this.createdAt.toEpochMilli(),
            updatedAt = this.updatedAt.toEpochMilli(),
        )
    }

    enum class ProjectsSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    fun enumerateProjects(
        parentId: IdType?,
        leaderId: IdType?,
        memberId: IdType?,
        sortBy: ProjectsSortBy,
        sortOrder: SortDirection,
        pageSize: Int,
        pageStart: IdType?,
    ): Pair<List<ProjectDTO>, PageDTO> {
        val spec =
            Specification.where<Project>(null)
                .and(
                    parentId?.let {
                        Specification.where { root, _, cb ->
                            cb.equal(root.get<IdType>("parent").get<IdType>("id"), it)
                        }
                    }
                )
                .and(
                    leaderId?.let {
                        Specification.where { root, _, cb ->
                            cb.equal(root.get<IdType>("leader").get<IdType>("id"), it)
                        }
                    }
                )
                .and(
                    memberId?.let {
                        Specification.where { root, _, cb ->
                            cb.isMember(it, root.get<Set<IdType>>("members"))
                        }
                    }
                )
        val cursorSpec =
            projectRepository
                .idSeekSpec(
                    Project::id,
                    sortProperty =
                        when (sortBy) {
                            ProjectsSortBy.CREATED_AT -> Project::createdAt
                            ProjectsSortBy.UPDATED_AT -> Project::updatedAt
                        },
                    direction = sortOrder.toJpaDirection(),
                )
                .specification(spec)
                .build()
        val result = projectRepository.findAllWithIdCursor(cursorSpec, pageStart, pageSize)
        return Pair(result.content.map { it.toProjectDTO() }, result.pageInfo.toPageDTO())
    }
}
