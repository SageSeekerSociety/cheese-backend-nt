package org.rucca.cheese.auth.dsl

import org.rucca.cheese.auth.core.*

/** DSL marker to ensure proper scoping of DSL methods. */
@DslMarker annotation class AuthDsl

/** Main DSL class for permission definitions. */
@AuthDsl
class PermissionsDsl<A : Action, R : ResourceType> {
    private val permissions = mutableListOf<Triple<Role, A, R>>()
    private val rules = mutableMapOf<Triple<Role, String, String>, PermissionRule<A, R>>()

    /**
     * Defines permissions for a role.
     *
     * @param role The role to define permissions for
     * @param block DSL block for role permissions
     */
    fun role(role: Role, block: RoleDsl<A, R>.() -> Unit) {
        val roleDsl = RoleDsl<A, R>(role)
        roleDsl.block()

        // Collect defined permissions
        permissions.addAll(roleDsl.permissions)

        // Collect defined rules
        rules.putAll(roleDsl.rules)
    }

    /** Builds the permission configuration. */
    internal fun build(): PermissionsConfig<A, R> {
        return PermissionsConfig(permissions, rules)
    }
}

/** DSL class for role permissions. */
@AuthDsl
class RoleDsl<A : Action, R : ResourceType>(private val role: Role) {
    internal val permissions = mutableListOf<Triple<Role, A, R>>()
    internal val rules = mutableMapOf<Triple<Role, String, String>, PermissionRule<A, R>>()

    /**
     * Defines actions that the role can perform.
     *
     * @param actions The actions
     * @return DSL for specifying resources
     */
    fun can(vararg actions: A): ActionTargetDsl<A, R> {
        return ActionTargetDsl(role, actions.toList(), this)
    }

    /** Adds a rule for an action and resource. */
    internal fun addRule(action: A, resource: R, rule: PermissionRule<A, R>) {
        val key = Triple(role, action.actionId, resource.typeName)
        rules[key] = rule
    }
}

/** DSL class for specifying resources for actions. */
@AuthDsl
class ActionTargetDsl<A : Action, R : ResourceType>(
    private val role: Role,
    private val actions: List<A>,
    private val roleDsl: RoleDsl<A, R>,
) {
    /**
     * Specifies the resources that the actions can be performed on.
     *
     * @param resources The resources
     * @return DSL for specifying conditions
     */
    fun on(vararg resources: R): PermissionConditionDsl<A, R> {
        // Add basic permissions
        for (action in actions) {
            for (resource in resources) {
                roleDsl.permissions.add(Triple(role, action, resource))
            }
        }

        return PermissionConditionDsl(role, actions, resources.toList(), roleDsl)
    }
}

/** DSL class for specifying conditions for permissions. */
@AuthDsl
class PermissionConditionDsl<A : Action, R : ResourceType>(
    private val role: Role,
    private val actions: List<A>,
    private val resources: List<R>,
    private val roleDsl: RoleDsl<A, R>,
) {
    /**
     * Specifies conditions for the permissions.
     *
     * @param block DSL block for defining conditions
     */
    fun where(block: PermissionRule<A, R>.() -> Unit) {
        for (action in actions) {
            for (resource in resources) {
                val rule = PermissionRule<A, R>().apply(block)
                roleDsl.addRule(action, resource, rule)
            }
        }
    }

    /** Adds a condition that the user must be the owner of the resource. */
    fun whenOwner() {
        where { ownerOnly() }
    }

    /** Indicates that no conditions are needed (syntactic sugar). */
    fun all() {
        // Do nothing, which means no conditions will be added
    }
}

/** Configuration result from permission DSL. */
data class PermissionsConfig<A : Action, R : ResourceType>(
    val permissions: List<Triple<Role, A, R>>,
    val rules: Map<Triple<Role, String, String>, PermissionRule<A, R>>,
)

/** Top-level function to define permissions using the DSL. */
fun <A : Action, R : ResourceType> definePermissions(
    block: PermissionsDsl<A, R>.() -> Unit
): PermissionsConfig<A, R> {
    val dsl = PermissionsDsl<A, R>()
    dsl.block()
    return dsl.build()
}
