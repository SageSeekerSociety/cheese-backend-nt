package org.rucca.cheese.project

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.context.ContextKey
import org.rucca.cheese.auth.context.DomainContextKeys
import org.rucca.cheese.auth.context.PermissionContextProvider
import org.rucca.cheese.auth.context.buildResourceContext
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.domain.DomainPermissionService
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.auth.dsl.applyHierarchy
import org.rucca.cheese.auth.dsl.definePermissions
import org.rucca.cheese.auth.dsl.defineRoleHierarchy
import org.rucca.cheese.auth.hierarchy.GraphRoleHierarchy
import org.rucca.cheese.auth.registry.RegistrationService
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.project.models.ProjectMemberRole
import org.rucca.cheese.team.TeamService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
object ProjectDomain : Domain {
    override val name: String = "project"
}

enum class ProjectAction(override val actionId: String) : Action {
    CREATE("create"),
    ENUMERATE("enumerate"),
    VIEW("view"),
    UPDATE("update"),
    DELETE("delete");

    override val domain: Domain = ProjectDomain
}

enum class ProjectResource(override val typeName: String) : ResourceType {
    PROJECT("project"),
    MEMBERSHIP("membership");

    override val domain: Domain = ProjectDomain

    companion object {
        fun of(typeName: String) =
            entries.find { it.typeName == typeName } ?: error("Invalid resource type")
    }
}

enum class ProjectRole(override val roleId: String) : Role {
    LEADER("leader"),
    MEMBER("member"),
    EXTERNAL("external");

    override val domain: Domain? = ProjectDomain
}

@Component
class ProjectRoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(ProjectRole.LEADER) { inheritsFrom(ProjectRole.MEMBER) }

            role(ProjectRole.MEMBER)

            role(ProjectRole.EXTERNAL)
        }

        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}

fun ProjectMemberRole.toRole(): ProjectRole =
    when (this) {
        ProjectMemberRole.LEADER -> ProjectRole.LEADER
        ProjectMemberRole.MEMBER -> ProjectRole.MEMBER
        ProjectMemberRole.EXTERNAL -> ProjectRole.EXTERNAL
    }

object ProjectContextKeys {
    val TEAM_ID = ContextKey.of<IdType>("teamId")
    val PROJECT_ID = ContextKey.of<IdType>("projectId")
    val IS_TEAM_MEMBER_PROVIDER = ContextKey.of<(IdType, IdType) -> Boolean>("isTeamMemberProvider")
}

@Component
class ProjectContextProvider(
    //    private val projectService: ProjectService,
    private val teamService: TeamService
) : PermissionContextProvider {
    override val domain: Domain = ProjectDomain

    override fun getContext(resourceName: String, resourceId: IdType?) =
        buildResourceContext(domain, ProjectResource.of(resourceName), resourceId) {
            ProjectContextKeys.IS_TEAM_MEMBER_PROVIDER { teamId, userId ->
                teamService.isTeamMember(teamId, userId)
            }
        }
}

@Component
class ProjectRoleProvider(private val projectService: ProjectService) : DomainRoleProvider {
    override val domain: Domain = ProjectDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val roles = mutableSetOf<Role>()

        when (DomainContextKeys.RESOURCE_TYPE.get(context)?.let { ProjectResource.of(it) }) {
            ProjectResource.PROJECT -> {
                val projectId = DomainContextKeys.RESOURCE_ID.get(context) ?: return roles
                roles.add(projectService.getMemberRole(projectId, userId).toRole())
            }

            ProjectResource.MEMBERSHIP -> {
                val projectId = ProjectContextKeys.PROJECT_ID.get(context) ?: return roles
                roles.add(projectService.getMemberRole(projectId, userId).toRole())
            }

            else -> {}
        }

        return roles.toSet()
    }
}

@Component
class ProjectPermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) : DomainPermissionService {
    private val logger = LoggerFactory.getLogger(ProjectPermissionConfig::class.java)

    override val domain: Domain = ProjectDomain

    private fun PermissionRule<ProjectAction, ProjectResource>.isTeamMember() {
        withCondition { userInfo, _, _, _, context ->
            val teamId = ProjectContextKeys.TEAM_ID.get(context) ?: return@withCondition false
            val isTeamMember =
                ProjectContextKeys.IS_TEAM_MEMBER_PROVIDER.get(context)
                    ?: return@withCondition false
            val userId = userInfo.userId
            isTeamMember(teamId, userId)
        }
    }

    @PostConstruct
    override fun configurePermissions() {
        // Register actions and resources
        registrationService.registerActions(*ProjectAction.entries.toTypedArray())
        registrationService.registerResources(*ProjectResource.entries.toTypedArray())

        // Define permissions
        val config = definePermissions {
            // User role permissions
            role(SystemRole.USER) {
                can(ProjectAction.CREATE, ProjectAction.ENUMERATE)
                    .on(ProjectResource.PROJECT)
                    .where { isTeamMember() }
            }

            // Admin role permissions
            role(SystemRole.ADMIN) {}

            role(ProjectRole.EXTERNAL) {
                can(ProjectAction.VIEW).on(ProjectResource.PROJECT).all()
                can(ProjectAction.ENUMERATE).on(ProjectResource.MEMBERSHIP).all()
            }

            role(ProjectRole.MEMBER) {
                can(ProjectAction.VIEW).on(ProjectResource.PROJECT).all()
                can(ProjectAction.ENUMERATE).on(ProjectResource.MEMBERSHIP).all()
            }

            role(ProjectRole.LEADER) {
                can(ProjectAction.UPDATE, ProjectAction.DELETE).on(ProjectResource.PROJECT).all()
                can(ProjectAction.CREATE, ProjectAction.UPDATE, ProjectAction.DELETE)
                    .on(ProjectResource.MEMBERSHIP)
                    .all()
            }
        }

        // Apply configuration
        permissionService.applyConfiguration(config)
    }
}
