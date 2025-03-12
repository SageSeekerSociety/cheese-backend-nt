package org.rucca.cheese.auth.core

import org.rucca.cheese.auth.domain.DomainRoleProviderRegistry
import org.rucca.cheese.auth.hierarchy.RoleHierarchy
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory

/**
 * Interface for permission evaluation. Implementors determine if a user has a specific permission.
 */
interface PermissionEvaluator {
    /**
     * Evaluates if a user has a permission for a resource.
     *
     * @param userInfo The user information
     * @param permission The permission to check
     * @param resourceId Optional ID of a specific resource instance
     * @param context Additional context for evaluation
     * @return true if the user has the permission, false otherwise
     */
    fun <A : Action, R : ResourceType> evaluate(
        userInfo: AuthUserInfo,
        permission: Permission<A, R>,
        resourceId: IdType? = null,
        context: Map<String, Any> = emptyMap(),
    ): Boolean
}

/**
 * Default implementation of PermissionEvaluator. Uses a permission service to get permissions for
 * user's roles and all parent roles, then evaluates the rule for the matching permission.
 *
 * @param permissionService The service providing role permissions
 * @param roleHierarchy The role hierarchy service
 */
class DefaultPermissionEvaluator(
    private val permissionService: PermissionConfigurationService,
    private val roleHierarchy: RoleHierarchy,
    private val roleProviderRegistry: DomainRoleProviderRegistry,
) : PermissionEvaluator {
    private val logger = LoggerFactory.getLogger(DefaultPermissionEvaluator::class.java)

    private fun Set<Role>.sorted(): List<Role> =
        this.sortedWith { a, b ->
            if (roleHierarchy.isParentRole(a, b)) 1
            else if (roleHierarchy.isParentRole(b, a)) -1 else 0
        }

    override fun <A : Action, R : ResourceType> evaluate(
        userInfo: AuthUserInfo,
        permission: Permission<A, R>,
        resourceId: IdType?,
        context: Map<String, Any>,
    ): Boolean {
        val (userId, userRoles) = userInfo

        userRoles.sorted().forEach { role ->
            if (checkRoleWithHierarchy(role, userInfo, permission, resourceId, context)) {
                logger.debug("Permission granted via system role: ${role.toDomainRoleId()}")
                return true
            }
        }

        val domainName = permission.action.domain.name
        val domainProvider = roleProviderRegistry.getProvider(domainName)

        if (domainProvider != null) {
            val domainRoles = domainProvider.getRoles(userId, context)

            // Check each domain role and its parent roles
            for (role in domainRoles) {
                if (checkRoleWithHierarchy(role, userInfo, permission, resourceId, context)) {
                    logger.debug("Permission granted via domain role: {}", role.toDomainRoleId())
                    return true
                }
            }
        }

        logger.debug("Permission denied for user {}: {}", userId, permission)
        return false
    }

    /** Checks if a role or any of its parent roles has the required permission. */
    private fun <A : Action, R : ResourceType> checkRoleWithHierarchy(
        role: Role,
        userInfo: AuthUserInfo,
        permission: Permission<A, R>,
        resourceId: IdType?,
        context: Map<String, Any>,
    ): Boolean {
        // Check the role itself
        if (checkPermission(role, userInfo, permission, resourceId, context)) {
            return true
        }

        // Check all parent roles
        val parentRoles = roleHierarchy.getAllParentRoles(role).sorted()
        return parentRoles.any { checkPermission(it, userInfo, permission, resourceId, context) }
    }

    /** Checks if a specific role has the required permission. */
    @Suppress("UNCHECKED_CAST")
    private fun <A : Action, R : ResourceType> checkPermission(
        role: Role,
        userInfo: AuthUserInfo,
        permission: Permission<A, R>,
        resourceId: IdType?,
        context: Map<String, Any>,
    ): Boolean {
        val permissions = permissionService.getPermissions(role)

        val matchingConfig =
            permissions.find { config ->
                config.action.domain.name == permission.action.domain.name &&
                    config.action.actionId == permission.action.actionId &&
                    config.resourceType.domain.name == permission.resourceType.domain.name &&
                    config.resourceType.typeName == permission.resourceType.typeName
            } as? PermissionConfig<A, R> ?: return false

        return matchingConfig.rule.evaluate(
            userInfo,
            matchingConfig.action,
            matchingConfig.resourceType,
            resourceId,
            context,
        )
    }
}
