package org.rucca.cheese.auth.dsl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.common.persistent.IdType

@DisplayName("Permission DSL Tests")
class PermissionDslTest {
    companion object {
        private val testDomain =
            object : Domain {
                override val name: String = "test"
            }
    }

    private enum class TestAction(override val actionId: String) : Action {
        VIEW("view"),
        EDIT("edit"),
        DELETE("delete");

        override val domain: Domain = testDomain
    }

    private enum class TestResource(override val typeName: String) : ResourceType {
        DOCUMENT("document"),
        FOLDER("folder");

        override val domain: Domain = testDomain
    }

    private enum class TestRole(override val roleId: String) : Role {
        ADMIN("admin"),
        EDITOR("editor"),
        VIEWER("viewer");

        override val domain: Domain = testDomain
    }

    @Test
    @DisplayName("Should create basic permission config with DSL")
    fun shouldCreateBasicPermissionConfig() {
        // Act
        val config =
            definePermissions<TestAction, TestResource> {
                role(TestRole.VIEWER) {
                    can(TestAction.VIEW).on(TestResource.DOCUMENT, TestResource.FOLDER).all()
                }
            }

        // Assert
        assertThat(config.permissions).hasSize(2)

        val permission1 = config.permissions[0]
        assertThat(permission1.first).isEqualTo(TestRole.VIEWER)
        assertThat(permission1.second).isEqualTo(TestAction.VIEW)
        assertThat(permission1.third).isEqualTo(TestResource.DOCUMENT)

        val permission2 = config.permissions[1]
        assertThat(permission2.first).isEqualTo(TestRole.VIEWER)
        assertThat(permission2.second).isEqualTo(TestAction.VIEW)
        assertThat(permission2.third).isEqualTo(TestResource.FOLDER)
    }

    @Test
    @DisplayName("Should create permission config with owner condition")
    fun shouldCreatePermissionConfigWithOwnerCondition() {
        // Act
        val config =
            definePermissions<TestAction, TestResource> {
                role(TestRole.EDITOR) { can(TestAction.EDIT).on(TestResource.DOCUMENT).whenOwner() }
            }

        // Assert
        assertThat(config.permissions).hasSize(1)

        val permission = config.permissions[0]
        assertThat(permission.first).isEqualTo(TestRole.EDITOR)
        assertThat(permission.second).isEqualTo(TestAction.EDIT)
        assertThat(permission.third).isEqualTo(TestResource.DOCUMENT)

        // Check rule exists
        val ruleKey = Triple(TestRole.EDITOR, "edit", "document")
        assertThat(config.rules).containsKey(ruleKey)

        // Verify rule behavior
        val rule = config.rules[ruleKey]!!

        // Owner check should pass
        val userInfo = AuthUserInfo(1L, setOf(TestRole.EDITOR))
        val ownerId = 1L
        val resourceId = 233L
        val context = mapOf("ownerIdProvider" to { _: IdType -> ownerId })

        assertThat(
                rule.evaluate(userInfo, TestAction.EDIT, TestResource.DOCUMENT, resourceId, context)
            )
            .isTrue()

        // Owner check should fail with different user
        val differentContext = mapOf("ownerIdProvider" to { _: IdType -> 2L })
        assertThat(
                rule.evaluate(
                    userInfo,
                    TestAction.EDIT,
                    TestResource.DOCUMENT,
                    resourceId,
                    differentContext,
                )
            )
            .isFalse()
    }

    @Test
    @DisplayName("Should create permission config with custom condition")
    fun shouldCreatePermissionConfigWithCustomCondition() {
        // Act
        val config =
            definePermissions<TestAction, TestResource> {
                role(TestRole.ADMIN) {
                    can(TestAction.DELETE).on(TestResource.DOCUMENT).where {
                        withCondition { _, _, _, _, context ->
                            val isImportant = context["isImportantDocument"] as? Boolean
                            isImportant != true // Can only delete non-important documents
                        }
                    }
                }
            }

        // Assert
        assertThat(config.permissions).hasSize(1)

        val permission = config.permissions[0]
        assertThat(permission.first).isEqualTo(TestRole.ADMIN)
        assertThat(permission.second).isEqualTo(TestAction.DELETE)
        assertThat(permission.third).isEqualTo(TestResource.DOCUMENT)

        // Check rule exists
        val ruleKey = Triple(TestRole.ADMIN, "delete", "document")
        assertThat(config.rules).containsKey(ruleKey)

        // Verify rule behavior
        val rule = config.rules[ruleKey]!!

        val userInfo = AuthUserInfo(1L, setOf(TestRole.ADMIN))
        val resourceId = 233L

        // Custom check should pass for non-important documents
        val context1 = mapOf("isImportantDocument" to false)
        assertThat(
                rule.evaluate(
                    userInfo,
                    TestAction.DELETE,
                    TestResource.DOCUMENT,
                    resourceId,
                    context1,
                )
            )
            .isTrue()

        // Custom check should fail for important documents
        val context2 = mapOf("isImportantDocument" to true)
        assertThat(
                rule.evaluate(
                    userInfo,
                    TestAction.DELETE,
                    TestResource.DOCUMENT,
                    resourceId,
                    context2,
                )
            )
            .isFalse()
    }

    @Test
    @DisplayName("Should create complex permission config with multiple roles and conditions")
    fun shouldCreateComplexPermissionConfig() {
        // Act
        val config =
            definePermissions<TestAction, TestResource> {
                // Admin can do everything
                role(TestRole.ADMIN) {
                    can(TestAction.VIEW, TestAction.EDIT, TestAction.DELETE)
                        .on(TestResource.DOCUMENT, TestResource.FOLDER)
                        .all()
                }

                // Editor can view all, edit all, but only delete own documents
                role(TestRole.EDITOR) {
                    can(TestAction.VIEW, TestAction.EDIT)
                        .on(TestResource.DOCUMENT, TestResource.FOLDER)
                        .all()

                    can(TestAction.DELETE).on(TestResource.DOCUMENT).whenOwner()
                }

                // Viewer can only view
                role(TestRole.VIEWER) {
                    can(TestAction.VIEW).on(TestResource.DOCUMENT, TestResource.FOLDER).all()
                }
            }

        // Assert
        // Should have 11 total permissions:
        // Admin: 3 actions * 2 resources = 6
        // Editor: 2 actions * 2 resources + 1 action * 1 resource = 5
        // Viewer: 1 action * 2 resources = 2
        // Total: 13
        assertThat(config.permissions).hasSize(13)

        // Check rule counts
        // Rules should exist for delete permissions with conditions
        val editorDeleteRule = Triple(TestRole.EDITOR, "delete", "document")
        assertThat(config.rules).containsKey(editorDeleteRule)
    }
}
