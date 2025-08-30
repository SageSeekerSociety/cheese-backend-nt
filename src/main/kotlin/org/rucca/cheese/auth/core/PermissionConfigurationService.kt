package org.rucca.cheese.auth.core

import org.rucca.cheese.auth.dsl.PermissionsConfig
import org.springframework.stereotype.Service

/**
 * Service for managing permission configurations. Stores permissions for different roles and
 * provides access to them.
 */
@Service
class PermissionConfigurationService {
    // Using Any as generic type to store all permission configs together
    private val rolePermissions = mutableMapOf<Role, MutableList<PermissionConfig<*, *>>>()

    /**
     * Adds a permission configuration for a role.
     *
     * @param role The role
     * @param config The permission configuration
     */
    fun <A : Action, R : ResourceType> addPermissionConfig(
        role: Role,
        config: PermissionConfig<A, R>,
    ) {
        val configs = rolePermissions.getOrPut(role) { mutableListOf() }
        configs.add(config)
    }

    /**
     * Adds a permission configuration with a simple rule for a role.
     *
     * @param role The role
     * @param action The action
     * @param resourceType The resource type
     * @param rule The permission rule
     */
    fun <A : Action, R : ResourceType> addPermission(
        role: Role,
        action: A,
        resourceType: R,
        rule: PermissionRule<A, R> = PermissionRule(),
    ) {
        val config = PermissionConfig(action, resourceType, rule)
        addPermissionConfig(role, config)
    }

    /**
     * Gets all permission configurations for a role.
     *
     * @param role The role
     * @return List of permission configurations
     */
    fun getPermissions(role: Role): List<PermissionConfig<*, *>> {
        return rolePermissions[role] ?: emptyList()
    }
}

fun <A : Action, R : ResourceType> PermissionConfigurationService.applyConfiguration(
    config: PermissionsConfig<A, R>
) {
    for (permission in config.permissions) {
        val (role, action, resource) = permission
        val ruleKey = Triple(role, action.actionId, resource.typeName)
        val rule = config.rules[ruleKey] ?: PermissionRule()
        addPermission(role, action, resource, rule)
    }
}
