package org.rucca.cheese.project

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.min
import org.hibernate.query.SortDirection
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.buildSpecification
import org.rucca.cheese.common.persistent.getProperty
import org.rucca.cheese.discussion.DiscussionReactionRepository
import org.rucca.cheese.discussion.DiscussionRepository
import org.rucca.cheese.model.*
import org.rucca.cheese.project.models.Project
import org.rucca.cheese.project.models.ProjectMemberRole
import org.rucca.cheese.project.models.ProjectMembership
import org.rucca.cheese.project.repositories.ProjectMembershipRepository
import org.rucca.cheese.project.repositories.ProjectRepository
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.user.services.UserService
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectMembershipRepository: ProjectMembershipRepository,
    private val teamService: TeamService,
    private val userService: UserService,
    private val entityPatcher: EntityPatcher,
    private val projectDiscussionRepository: DiscussionRepository,
    private val projectDiscussionReactionRepository: DiscussionReactionRepository,
) {
    @Cacheable("projectMemberRole", key = "#projectId + ':' + #userId")
    fun getMemberRole(projectId: IdType, userId: IdType): ProjectMemberRole {
        val membership =
            projectMembershipRepository.findRoleByProjectIdAndUserId(projectId, userId)
                ?: throw NotFoundError(
                    "Project Membership not found with projectId $projectId and userId $userId"
                )
        return membership.role
    }

    private fun ProjectMemberRole.toDTO(): ProjectMemberRoleDTO {
        return ProjectMemberRoleDTO.valueOf(this.name)
    }

    private fun ProjectMembership.toDTO(): ProjectMembershipDTO =
        ProjectMembershipDTO(
            user = userService.getUserDto(this.userId),
            role = this.role.toDTO(),
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = this.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            notes = this.notes,
        )

    fun getMembers(projectId: IdType): List<ProjectMembershipDTO> {
        return projectMembershipRepository.findAllByProjectId(projectId).map { it.toDTO() }
    }

    fun addMember(
        projectId: IdType,
        userId: IdType,
        role: ProjectMemberRole,
        notes: String? = null,
    ): ProjectMembershipDTO {
        if (role == ProjectMemberRole.LEADER) {
            throw ForbiddenError("Cannot add a leader to a project")
        }
        userService.ensureUsersExist(listOf(userId))
        val project =
            projectRepository.findById(projectId).orElseThrow {
                NotFoundError("project", projectId)
            }
        val membership =
            ProjectMembership(project = project, userId = userId, role = role, notes = notes)
        project.memberships.add(membership)
        projectRepository.save(project)
        return membership.toDTO()
    }

    fun removeMember(projectId: IdType, userId: IdType) {
        val project =
            projectRepository.findById(projectId).orElseThrow {
                NotFoundError("project", projectId)
            }
        val membership =
            project.memberships.find { it.userId == userId }
                ?: throw NotFoundError("project membership", userId)
        project.memberships.remove(membership)
        projectRepository.save(project)
    }

    fun getProject(projectId: IdType): ProjectDTO {
        val project =
            projectRepository.findByIdOrNull(projectId) ?: throw NotFoundError("project", projectId)

        return project.toProjectDTO()
    }

    @Transactional
    fun deleteProject(projectId: IdType) {
        val project =
            projectRepository.findById(projectId).orElseThrow {
                NotFoundError("project", projectId)
            }

        projectRepository.delete(project)
    }

    @Transactional
    fun patchProject(
        projectId: IdType,
        patchProjectRequestDTO: PatchProjectRequestDTO,
    ): ProjectDTO {
        val project =
            projectRepository.findById(projectId).orElseThrow {
                NotFoundError("project", projectId)
            }

        entityPatcher.patch(project, patchProjectRequestDTO) {
            handle(PatchProjectRequestDTO::startDate) { entity, startDate ->
                entity.startDate =
                    OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(startDate),
                        ZoneId.systemDefault(),
                    )
            }
            handle(PatchProjectRequestDTO::endDate) { entity, endDate ->
                entity.endDate =
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(endDate), ZoneId.systemDefault())
            }
        }

        return projectRepository.save(project).toProjectDTO()
    }

    @Transactional
    fun createProject(
        name: String,
        description: String,
        colorCode: String,
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        teamId: IdType,
        leaderId: IdType,
        parentId: IdType? = null,
        externalTaskId: IdType? = null,
        githubRepo: String? = null,
        memberIds: List<IdType> = emptyList(),
        externalCollaboratorIds: List<IdType> = emptyList(),
    ): ProjectDTO {
        teamService.ensureTeamIdExists(teamId)
        userService.ensureUsersExist(memberIds + externalCollaboratorIds + leaderId)
        val project =
            Project(
                name = name,
                description = description,
                colorCode = colorCode,
                startDate = startDate,
                endDate = endDate,
                teamId = teamId,
                leaderId = leaderId,
                parent = parentId?.let { projectRepository.getReferenceById(it) },
                externalTaskId = externalTaskId,
                githubRepo = githubRepo,
            )

        project.addMember(leaderId, ProjectMemberRole.LEADER)

        memberIds.forEach { memberId ->
            if (memberId != leaderId) {
                project.addMember(memberId, ProjectMemberRole.MEMBER)
            }
        }

        externalCollaboratorIds.forEach { collaboratorId ->
            if (collaboratorId != leaderId && collaboratorId !in memberIds) {
                project.addMember(collaboratorId, ProjectMemberRole.EXTERNAL)
            }
        }

        return projectRepository.save(project).toProjectDTO()
    }

    private fun Project.toProjectDTO(): ProjectDTO {
        return ProjectDTO(
            id = this.id!!,
            name = this.name,
            description = this.description,
            colorCode = this.colorCode,
            startDate = this.startDate.toEpochMilli(),
            endDate = this.endDate.toEpochMilli(),
            leader = userService.getUserDto(this.leaderId),
            team = teamService.getTeamDto(this.teamId),
            parentId = this.parent?.id,
            externalTaskId = this.externalTaskId,
            githubRepo = this.githubRepo,
            createdAt = this.createdAt.toEpochMilli(),
            updatedAt = this.updatedAt.toEpochMilli(),
            members =
                ProjectMembersDTO(
                    count = this.memberships.size,
                    examples =
                        this.memberships.take(min(this.memberships.size, 5)).map {
                            ProjectMembershipDTO(
                                user = userService.getUserDto(it.userId),
                                role = ProjectMemberRoleDTO.valueOf(it.role.name),
                                notes = it.notes,
                                createdAt =
                                    it.createdAt
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli(),
                                updatedAt =
                                    it.updatedAt
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli(),
                            )
                        },
                ),
            archived = this.archived,
        )
    }

    enum class ProjectsSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    fun enumerateProjects(
        teamId: IdType,
        parentId: IdType?,
        leaderId: IdType?,
        memberId: IdType?,
        archived: Boolean?,
        sortBy: ProjectsSortBy,
        sortOrder: SortDirection,
    ): List<ProjectDTO> {
        val spec = buildSpecification {
            where { root, _, cb -> cb.equal(root.getProperty(Project::teamId), teamId) }
            if (parentId != null) {
                and { root, _, cb ->
                    cb.equal(root.getProperty(Project::parent).getProperty(Project::id), parentId)
                }
            }
            if (leaderId != null) {
                and { root, _, cb -> cb.equal(root.getProperty(Project::leaderId), leaderId) }
            }
            if (archived != null) {
                and { root, _, cb -> cb.equal(root.getProperty(Project::archived), archived) }
            }
        }
        val sortProperty =
            when (sortBy) {
                ProjectsSortBy.CREATED_AT -> Project::createdAt
                ProjectsSortBy.UPDATED_AT -> Project::updatedAt
            }
        val direction = sortOrder.toJpaDirection()
        Sort.by(direction, sortProperty.name)
        val result = projectRepository.findAll(spec, Sort.by(direction, sortProperty.name))

        val projectDTOs = result.map { it.toProjectDTO() }
        val rootProjects = mutableListOf<ProjectDTO>()
        val childrenMap = mutableMapOf<IdType, MutableList<ProjectDTO>>()

        projectDTOs.forEach { project ->
            if (project.parentId == null) {
                rootProjects.add(project)
            } else {
                if (!childrenMap.containsKey(project.parentId)) {
                    childrenMap[project.parentId] = mutableListOf()
                }
                childrenMap[project.parentId]?.add(project)
            }
        }

        rootProjects.forEach { rootProject ->
            val children = childrenMap[rootProject.id] ?: emptyList()
            val updatedRootProject = rootProject.copy(children = children)
            rootProjects[rootProjects.indexOf(rootProject)] = updatedRootProject
        }

        return rootProjects
    }
}
