package org.rucca.cheese.auth.hierarchy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.Role

@DisplayName("Graph Role Hierarchy Tests")
class GraphRoleHierarchyTest {

    // Test roles
    private enum class TestRole(override val roleId: String) : Role {
        ADMIN("admin"),
        MANAGER("manager"),
        TEAM_LEAD("team_lead"),
        DEVELOPER("developer"),
        VIEWER("viewer");

        override val domain: Domain? = null
    }

    // SUT
    private lateinit var roleHierarchy: GraphRoleHierarchy

    @BeforeEach
    fun setUp() {
        roleHierarchy = GraphRoleHierarchy()

        // Setup hierarchy:
        // ADMIN
        //   |
        // MANAGER
        //   |
        // TEAM_LEAD
        //  /   \
        // DEVELOPER  VIEWER

        roleHierarchy.addParentChildRelationship(TestRole.ADMIN, TestRole.MANAGER)
        roleHierarchy.addParentChildRelationship(TestRole.MANAGER, TestRole.TEAM_LEAD)
        roleHierarchy.addParentChildRelationship(TestRole.TEAM_LEAD, TestRole.DEVELOPER)
        roleHierarchy.addParentChildRelationship(TestRole.TEAM_LEAD, TestRole.VIEWER)
    }

    @Test
    @DisplayName("Should get direct parent roles")
    fun shouldGetDirectParentRoles() {
        // Act
        val developerParents = roleHierarchy.getAllParentRoles(TestRole.DEVELOPER)
        val teamLeadParents = roleHierarchy.getAllParentRoles(TestRole.TEAM_LEAD)
        val adminParents = roleHierarchy.getAllParentRoles(TestRole.ADMIN)

        // Assert
        // Developer should have TEAM_LEAD, MANAGER and ADMIN as parents
        assertThat(developerParents)
            .containsExactlyInAnyOrder(TestRole.TEAM_LEAD, TestRole.MANAGER, TestRole.ADMIN)

        // Team Lead should have MANAGER and ADMIN as parents
        assertThat(teamLeadParents).containsExactlyInAnyOrder(TestRole.MANAGER, TestRole.ADMIN)

        // Admin should have no parents
        assertThat(adminParents).isEmpty()
    }

    @Test
    @DisplayName("Should detect parent-child relationships")
    fun shouldDetectParentChildRelationships() {
        // Act & Assert
        assertThat(roleHierarchy.isParentRole(TestRole.ADMIN, TestRole.DEVELOPER)).isTrue()
        assertThat(roleHierarchy.isParentRole(TestRole.TEAM_LEAD, TestRole.DEVELOPER)).isTrue()
        assertThat(roleHierarchy.isParentRole(TestRole.DEVELOPER, TestRole.ADMIN)).isFalse()
        assertThat(roleHierarchy.isParentRole(TestRole.VIEWER, TestRole.DEVELOPER)).isFalse()
    }

    @Test
    @DisplayName("Should prevent circular references")
    fun shouldPreventCircularReferences() {
        // Arrange - try to create a circular reference
        // This would create: DEVELOPER -> ADMIN -> MANAGER -> TEAM_LEAD -> DEVELOPER

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            roleHierarchy.addParentChildRelationship(TestRole.DEVELOPER, TestRole.ADMIN)
        }
    }

    @Test
    @DisplayName("Should prevent self-references")
    fun shouldPreventSelfReferences() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            roleHierarchy.addParentChildRelationship(TestRole.ADMIN, TestRole.ADMIN)
        }
    }
}
