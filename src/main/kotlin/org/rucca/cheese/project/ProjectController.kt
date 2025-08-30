package org.rucca.cheese.project

import org.hibernate.query.SortDirection
import org.rucca.cheese.api.ProjectsApi
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.AuthContext
import org.rucca.cheese.auth.spring.ResourceId
import org.rucca.cheese.common.helper.toOffsetDateTime
import org.rucca.cheese.model.*
import org.rucca.cheese.project.models.ProjectMemberRole
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseNewAuth
class ProjectController(private val projectService: ProjectService) : ProjectsApi {
    @Auth("project:create:project")
    override fun createProject(
        @AuthContext("teamId", field = "teamId") createProjectRequestDTO: CreateProjectRequestDTO
    ): ResponseEntity<CreateProject201ResponseDTO> {
        val projectDTO =
            projectService.createProject(
                name = createProjectRequestDTO.name,
                description = createProjectRequestDTO.description,
                colorCode = createProjectRequestDTO.colorCode,
                startDate = createProjectRequestDTO.startDate.toOffsetDateTime(),
                endDate = createProjectRequestDTO.endDate.toOffsetDateTime(),
                teamId = createProjectRequestDTO.teamId,
                leaderId = createProjectRequestDTO.leaderId,
                parentId = createProjectRequestDTO.parentId,
                externalTaskId = createProjectRequestDTO.externalTaskId,
                githubRepo = createProjectRequestDTO.githubRepo,
            )
        return ResponseEntity.ok(
            CreateProject201ResponseDTO(data = CreateProject201ResponseDataDTO(projectDTO))
        )
    }

    @Auth("project:enumerate:project")
    override fun getProjects(
        @AuthContext("teamId") teamId: Long,
        @AuthContext("parentId") parentId: Long?,
        leaderId: Long?,
        memberId: Long?,
        archived: Boolean?,
    ): ResponseEntity<GetProjects200ResponseDTO> {
        val sortBy = ProjectService.ProjectsSortBy.CREATED_AT // Default sort by createdAt
        val sortOrder = SortDirection.ASCENDING // Default sort order

        val projects =
            projectService.enumerateProjects(
                teamId = teamId,
                parentId = parentId,
                leaderId = leaderId,
                memberId = memberId,
                archived = archived,
                sortBy = sortBy,
                sortOrder = sortOrder,
            )

        return ResponseEntity.ok(
            GetProjects200ResponseDTO(data = GetProjects200ResponseDataDTO(projects))
        )
    }

    @Auth("project:update:project")
    override fun patchProject(
        @ResourceId projectId: Long,
        patchProjectRequestDTO: PatchProjectRequestDTO,
    ): ResponseEntity<GetProject200ResponseDTO> {
        val projectDTO = projectService.patchProject(projectId, patchProjectRequestDTO)
        return ResponseEntity.ok(
            GetProject200ResponseDTO(data = CreateProject201ResponseDataDTO(projectDTO))
        )
    }

    @Auth("project:delete:project")
    override fun deleteProject(@ResourceId projectId: Long): ResponseEntity<Unit> {
        projectService.deleteProject(projectId)
        return ResponseEntity.noContent().build()
    }

    @Auth("project:view:project")
    override fun getProject(@ResourceId projectId: Long): ResponseEntity<GetProject200ResponseDTO> {
        val projectDTO = projectService.getProject(projectId)
        return ResponseEntity.ok(
            GetProject200ResponseDTO(
                code = 200,
                message = "OK",
                data = CreateProject201ResponseDataDTO(projectDTO),
            )
        )
    }

    @Auth("project:enumerate:membership")
    override fun getProjectMembers(
        @AuthContext("projectId") projectId: Long,
        pageStart: Long?,
        pageSize: Int,
    ): ResponseEntity<GetProjectMembers200ResponseDTO> {
        val members = projectService.getMembers(projectId)
        return ResponseEntity.ok(
            GetProjectMembers200ResponseDTO(
                code = 200,
                message = "OK",
                data = GetProjectMembers200ResponseDataDTO(members = members),
            )
        )
    }

    private fun ProjectMemberRoleDTO.toRole(): ProjectMemberRole =
        when (this) {
            ProjectMemberRoleDTO.LEADER -> ProjectMemberRole.LEADER
            ProjectMemberRoleDTO.MEMBER -> ProjectMemberRole.MEMBER
            ProjectMemberRoleDTO.EXTERNAL -> ProjectMemberRole.EXTERNAL
        }

    @Auth("project:create:membership")
    override fun postProjectMember(
        projectId: Long,
        postProjectMemberRequestDTO: PostProjectMemberRequestDTO,
    ): ResponseEntity<PostProjectMember201ResponseDTO> {
        val membershipDTO =
            projectService.addMember(
                projectId = projectId,
                userId = postProjectMemberRequestDTO.userId,
                role = postProjectMemberRequestDTO.role.toRole(),
                notes = postProjectMemberRequestDTO.notes,
            )

        return ResponseEntity.ok(
            PostProjectMember201ResponseDTO(
                code = 201,
                message = "Created",
                data = PostProjectMember201ResponseDataDTO(membershipDTO),
            )
        )
    }

    @Auth("project:delete:membership")
    override fun deleteProjectMember(projectId: Long, userId: Long): ResponseEntity<Unit> {
        projectService.removeMember(projectId, userId)
        return ResponseEntity.noContent().build()
    }
}
