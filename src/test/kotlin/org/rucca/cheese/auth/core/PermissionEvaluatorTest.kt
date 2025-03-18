package org.rucca.cheese.auth.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.auth.domain.DomainRoleProviderRegistry
import org.rucca.cheese.auth.hierarchy.RoleHierarchy
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.auth.spring.UserSecurityService
import org.rucca.cheese.common.persistent.IdType

@DisplayName("Permission Evaluator Tests")
class PermissionEvaluatorTest {

    // Test domains
    companion object {
        private val testDomain =
            object : Domain {
                override val name: String = "test"
            }
    }

    // Test actions
    private enum class TestAction(override val actionId: String, override val domain: Domain) :
        Action {
        VIEW("view", testDomain),
        EDIT("edit", testDomain),
        DELETE("delete", testDomain),
    }

    // Test resources
    private enum class TestResource(override val typeName: String, override val domain: Domain) :
        ResourceType {
        DOCUMENT("document", testDomain),
        FOLDER("folder", testDomain),
    }

    // Test roles
    private enum class TestRole(override val roleId: String) : Role {
        ADMIN("admin"),
        EDITOR("editor"),
        VIEWER("viewer");

        override val domain: Domain? = testDomain
    }

    // Mocks
    private lateinit var permissionService: PermissionConfigurationService
    private lateinit var userSecurityService: UserSecurityService
    private lateinit var roleProviderRegistry: DomainRoleProviderRegistry
    private lateinit var roleHierarchy: RoleHierarchy

    // SUT
    private lateinit var permissionEvaluator: DefaultPermissionEvaluator

    @BeforeEach
    fun setUp() {
        permissionService = mock(PermissionConfigurationService::class.java)
        userSecurityService = mock(UserSecurityService::class.java)
        roleProviderRegistry = mock(DomainRoleProviderRegistry::class.java)
        roleHierarchy = mock(RoleHierarchy::class.java)

        permissionEvaluator =
            DefaultPermissionEvaluator(permissionService, roleHierarchy, roleProviderRegistry)
    }

    @Nested
    @DisplayName("Basic Permission Tests")
    inner class BasicPermissionTests {

        @Test
        @DisplayName("Should grant permission when user role has direct permission")
        fun shouldGrantPermissionWhenUserRoleHasDirectPermission() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.VIEWER))
            val (userId, userRoles) = userInfo
            val permission = Permission(TestAction.VIEW, TestResource.DOCUMENT)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(roleHierarchy.getAllParentRoles(TestRole.VIEWER)).thenReturn(emptySet())

            // Mock permission configuration
            val rule = PermissionRule<TestAction, TestResource>()
            val config = PermissionConfig(TestAction.VIEW, TestResource.DOCUMENT, rule)

            `when`(permissionService.getPermissions(TestRole.VIEWER)).thenReturn(listOf(config))

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, null, emptyMap())

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("Should deny permission when user has no matching permission")
        fun shouldDenyPermissionWhenUserHasNoMatchingPermission() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.VIEWER))
            val (userId, userRoles) = userInfo
            val permission = Permission(TestAction.DELETE, TestResource.DOCUMENT)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(roleHierarchy.getAllParentRoles(TestRole.VIEWER)).thenReturn(emptySet())

            // Mock permission configuration - viewer can only view
            val rule = PermissionRule<TestAction, TestResource>()
            val config = PermissionConfig(TestAction.VIEW, TestResource.DOCUMENT, rule)

            `when`(permissionService.getPermissions(TestRole.VIEWER)).thenReturn(listOf(config))

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, null, emptyMap())

            // Assert
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("Role Hierarchy Tests")
    inner class RoleHierarchyTests {

        @Test
        @DisplayName(
            "Should grant permission from parent role when user role doesn't have direct permission"
        )
        fun shouldGrantPermissionFromParentRole() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.EDITOR))
            val (userId, userRoles) = userInfo
            val permission = Permission(TestAction.EDIT, TestResource.DOCUMENT)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)

            // Editor role doesn't have direct permission
            `when`(permissionService.getPermissions(TestRole.EDITOR)).thenReturn(emptyList())

            // But its parent role (ADMIN) does
            `when`(roleHierarchy.getAllParentRoles(TestRole.EDITOR))
                .thenReturn(setOf(TestRole.ADMIN))

            val rule = PermissionRule<TestAction, TestResource>()
            val config = PermissionConfig(TestAction.EDIT, TestResource.DOCUMENT, rule)

            `when`(permissionService.getPermissions(TestRole.ADMIN)).thenReturn(listOf(config))

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, null, emptyMap())

            // Assert
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("Domain Role Tests")
    inner class DomainRoleTests {

        @Test
        @DisplayName("Should grant permission when user has domain-specific role with permission")
        fun shouldGrantPermissionFromDomainRole() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.VIEWER))
            val (userId, userRoles) = userInfo
            val permission = Permission(TestAction.DELETE, TestResource.DOCUMENT)
            val resourceId = 123L

            // User's system role doesn't have the permission
            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(permissionService.getPermissions(TestRole.VIEWER)).thenReturn(emptyList())
            `when`(roleHierarchy.getAllParentRoles(TestRole.VIEWER)).thenReturn(emptySet())

            // But user has a domain-specific role (EDITOR) for this document
            val domainRoleProvider = mock(DomainRoleProvider::class.java)
            `when`(domainRoleProvider.getRoles(eq(userId), anyMap()))
                .thenReturn(setOf(TestRole.EDITOR))
            `when`(roleProviderRegistry.getProvider(testDomain.name)).thenReturn(domainRoleProvider)

            // And the EDITOR role has DELETE permission
            val rule = PermissionRule<TestAction, TestResource>()
            val config = PermissionConfig(TestAction.DELETE, TestResource.DOCUMENT, rule)
            `when`(permissionService.getPermissions(TestRole.EDITOR)).thenReturn(listOf(config))
            `when`(roleHierarchy.getAllParentRoles(TestRole.EDITOR)).thenReturn(emptySet())

            // Act
            val result =
                permissionEvaluator.evaluate(
                    userInfo,
                    permission,
                    resourceId,
                    mapOf("documentId" to resourceId),
                )

            // Assert
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("Permission Rule Tests")
    inner class PermissionRuleTests {

        @Test
        @DisplayName("Should enforce owner check in permission rule")
        fun shouldEnforceOwnerCheck() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.VIEWER))
            val (userId, userRoles) = userInfo
            val ownerId = userId // Same user, should pass
            val resourceId = 123L
            val permission = Permission(TestAction.EDIT, TestResource.DOCUMENT)

            // Create a rule with owner check
            val rule = PermissionRule<TestAction, TestResource>().apply { ownerOnly() }

            val config = PermissionConfig(TestAction.EDIT, TestResource.DOCUMENT, rule)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(permissionService.getPermissions(TestRole.VIEWER)).thenReturn(listOf(config))
            `when`(roleHierarchy.getAllParentRoles(TestRole.VIEWER)).thenReturn(emptySet())

            // Context with owner information
            val context = mapOf("ownerIdProvider" to { _: IdType -> ownerId })

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, resourceId, context)

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("Should deny when owner check fails in permission rule")
        fun shouldDenyWhenOwnerCheckFails() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.VIEWER))
            val (userId, userRoles) = userInfo
            val ownerId = 234L // Different user, should fail
            val resourceId = 123L
            val permission = Permission(TestAction.EDIT, TestResource.DOCUMENT)

            // Create a rule with owner check
            val rule = PermissionRule<TestAction, TestResource>().apply { ownerOnly() }

            val config = PermissionConfig(TestAction.EDIT, TestResource.DOCUMENT, rule)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(permissionService.getPermissions(TestRole.VIEWER)).thenReturn(listOf(config))
            `when`(roleHierarchy.getAllParentRoles(TestRole.VIEWER)).thenReturn(emptySet())

            // Context with owner information
            val context = mapOf("ownerIdProvider" to { _: IdType -> ownerId })

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, resourceId, context)

            // Assert
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("Should enforce custom condition in permission rule")
        fun shouldEnforceCustomCondition() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.EDITOR))
            val (userId, userRoles) = userInfo
            val resourceId = 123L
            val permission = Permission(TestAction.EDIT, TestResource.DOCUMENT)

            // Create a rule with custom condition checking document status
            val rule =
                PermissionRule<TestAction, TestResource>().apply {
                    withCondition { _, _, _, _, context ->
                        val status =
                            (context["documentStatusProvider"] as? (IdType) -> String)?.invoke(
                                resourceId
                            )
                        status == "DRAFT"
                    }
                }

            val config = PermissionConfig(TestAction.EDIT, TestResource.DOCUMENT, rule)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(permissionService.getPermissions(TestRole.EDITOR)).thenReturn(listOf(config))
            `when`(roleHierarchy.getAllParentRoles(TestRole.EDITOR)).thenReturn(emptySet())

            // Context with document status information
            val context = mapOf("documentStatusProvider" to { _: IdType -> "DRAFT" })

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, resourceId, context)

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("Should combine conditions with AND logic")
        fun shouldCombineConditionsWithAndLogic() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.EDITOR))
            val (userId, userRoles) = userInfo
            val ownerId = userId
            val resourceId = 123L
            val permission = Permission(TestAction.EDIT, TestResource.DOCUMENT)

            // Create a rule with multiple conditions
            val rule =
                PermissionRule<TestAction, TestResource>().apply {
                    ownerOnly() // Must be owner
                    withCondition { _, _, _, _, context ->
                        val status =
                            (context["documentStatusProvider"] as? (IdType) -> String)?.invoke(
                                resourceId
                            )
                        status == "DRAFT" // AND document must be in DRAFT status
                    }
                }

            val config = PermissionConfig(TestAction.EDIT, TestResource.DOCUMENT, rule)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(permissionService.getPermissions(TestRole.EDITOR)).thenReturn(listOf(config))
            `when`(roleHierarchy.getAllParentRoles(TestRole.EDITOR)).thenReturn(emptySet())

            // Context with both required information
            val context =
                mapOf(
                    "ownerIdProvider" to { _: IdType -> ownerId },
                    "documentStatusProvider" to { _: IdType -> "DRAFT" },
                )

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, resourceId, context)

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("Should fail when any combined condition fails")
        fun shouldFailWhenAnyCombinedConditionFails() {
            // Arrange
            val userInfo = AuthUserInfo(123L, setOf(TestRole.EDITOR))
            val (userId, userRoles) = userInfo
            val ownerId = userId // Owner check passes
            val resourceId = 123L
            val permission = Permission(TestAction.EDIT, TestResource.DOCUMENT)

            // Create a rule with multiple conditions
            val rule =
                PermissionRule<TestAction, TestResource>().apply {
                    ownerOnly() // Must be owner (passes)
                    withCondition { _, _, _, _, context ->
                        val status =
                            (context["documentStatusProvider"] as? (IdType) -> String)?.invoke(
                                resourceId
                            )
                        status == "DRAFT" // But document is in PUBLISHED status (fails)
                    }
                }

            val config = PermissionConfig(TestAction.EDIT, TestResource.DOCUMENT, rule)

            `when`(userSecurityService.getUserRoles(userId)).thenReturn(userRoles)
            `when`(permissionService.getPermissions(TestRole.EDITOR)).thenReturn(listOf(config))
            `when`(roleHierarchy.getAllParentRoles(TestRole.EDITOR)).thenReturn(emptySet())

            // Context with both required information, but status condition will fail
            val context =
                mapOf(
                    "ownerIdProvider" to { _: IdType -> ownerId },
                    "documentStatusProvider" to { _: IdType -> "PUBLISHED" },
                )

            // Act
            val result = permissionEvaluator.evaluate(userInfo, permission, resourceId, context)

            // Assert
            assertThat(result).isFalse()
        }
    }
}
