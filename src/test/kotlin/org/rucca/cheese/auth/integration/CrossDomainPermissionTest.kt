package org.rucca.cheese.auth.integration

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.auth.domain.DomainRoleProviderRegistry
import org.rucca.cheese.auth.hierarchy.RoleHierarchy
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.spring.UserSecurityService

/**
 * Integration test for cross-domain permissions. Tests the scenario where Project permissions
 * depend on Team membership.
 */
@DisplayName("Cross Domain Permission Tests")
class CrossDomainPermissionTest {
    // Test domains
    companion object {
        private val teamDomain =
            object : Domain {
                override val name: String = "team"
            }

        private val projectDomain =
            object : Domain {
                override val name: String = "project"
            }
    }

    // Test actions
    private object CreateProjectAction : Action {
        override val actionId: String = "create"
        override val domain: Domain = projectDomain
    }

    // Test resources
    private object ProjectResource : ResourceType {
        override val typeName: String = "project"
        override val domain: Domain = projectDomain
    }

    // Test roles
    private enum class TeamRole(override val roleId: String) : Role {
        TEAM_ADMIN("team_admin"),
        TEAM_MEMBER("team_member");

        override val domain: Domain? = teamDomain
    }

    private enum class ProjectRole(override val roleId: String) : Role {
        PROJECT_CREATOR("project_creator");

        override val domain: Domain? = projectDomain
    }

    // Mocks
    @MockK private lateinit var permissionService: PermissionConfigurationService

    @MockK private lateinit var userSecurityService: UserSecurityService

    @MockK private lateinit var roleHierarchy: RoleHierarchy

    @MockK private lateinit var roleProviderRegistry: DomainRoleProviderRegistry

    @MockK private lateinit var teamRoleProvider: DomainRoleProvider

    @MockK private lateinit var projectRoleProvider: DomainRoleProvider

    // SUT
    private lateinit var permissionEvaluator: PermissionEvaluator

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        permissionEvaluator =
            DefaultPermissionEvaluator(permissionService, roleHierarchy, roleProviderRegistry)

        // Setup domain role providers
        every { teamRoleProvider.domain } returns teamDomain
        every { projectRoleProvider.domain } returns projectDomain

        every { roleProviderRegistry.getProvider(teamDomain.name) } returns teamRoleProvider
        every { roleProviderRegistry.getProvider(projectDomain.name) } returns projectRoleProvider

        // Setup empty parent roles for all roles
        every { roleHierarchy.getAllParentRoles(any()) } returns emptySet()
    }

    @Test
    @DisplayName("Should allow creating project when user is team member")
    fun shouldAllowCreatingProjectWhenUserIsTeamMember() {
        // Arrange
        val userInfo = AuthUserInfo(123L, setOf(SystemRole.USER))
        val (userId, userRoles) = userInfo
        val teamId = 456L
        val permission = Permission(CreateProjectAction, ProjectResource)

        // User's global role doesn't have project creation permission
        every { userSecurityService.getUserRoles(userId) } returns userRoles
        every { permissionService.getPermissions(SystemRole.USER) } returns emptyList()

        // But user is a team member
        every { teamRoleProvider.getRoles(userId, any()) } returns setOf(TeamRole.TEAM_MEMBER)

        // And team members are allowed to create projects
        val projectCreatorRule = PermissionRule<Action, ResourceType>()
        val projectCreatorConfig =
            PermissionConfig(CreateProjectAction, ProjectResource, projectCreatorRule)
        every { permissionService.getPermissions(ProjectRole.PROJECT_CREATOR) } returns
            listOf(projectCreatorConfig)

        // Project role provider gives PROJECT_CREATOR role to team members
        every { projectRoleProvider.getRoles(userId, any()) } returns
            setOf(ProjectRole.PROJECT_CREATOR)

        // Context with team ID
        val context = mapOf("teamId" to teamId)

        // Act
        val result = permissionEvaluator.evaluate(userInfo, permission, null, context)

        // Assert
        assertThat(result).isTrue()

        // Verify project role provider was called with context
        verify { projectRoleProvider.getRoles(userId, any()) }
    }

    @Test
    @DisplayName("Should deny creating project when user is not team member")
    fun shouldDenyCreatingProjectWhenUserIsNotTeamMember() {
        // Arrange
        val userInfo = AuthUserInfo(123L, setOf(SystemRole.USER))
        val (userId, userRoles) = userInfo
        val teamId = 456L
        val permission = Permission(CreateProjectAction, ProjectResource)

        // User's global role doesn't have project creation permission
        every { userSecurityService.getUserRoles(userId) } returns userRoles
        every { permissionService.getPermissions(SystemRole.USER) } returns emptyList()

        // User is not a team member
        every { teamRoleProvider.getRoles(userId, any()) } returns emptySet()

        // Project role provider gives no roles
        every { projectRoleProvider.getRoles(userId, any()) } returns emptySet()

        // Context with team ID
        val context = mapOf("teamId" to teamId)

        // Act
        val result = permissionEvaluator.evaluate(userInfo, permission, null, context)

        // Assert
        assertThat(result).isFalse()
    }
}
