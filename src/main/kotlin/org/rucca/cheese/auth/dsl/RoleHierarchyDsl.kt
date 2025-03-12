package org.rucca.cheese.auth.dsl

import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.hierarchy.GraphRoleHierarchy

/** DSL for defining role hierarchy. */
@AuthDsl
class RoleHierarchyDsl {
    private val relationships = mutableListOf<Pair<Role, Role>>() // parent, child

    /**
     * Defines a role in the hierarchy.
     *
     * @param child The role to define
     * @param block DSL block for role definition
     */
    fun role(child: Role, block: RoleDefinitionDsl.() -> Unit = {}) {
        val dsl = RoleDefinitionDsl(child)
        dsl.block()

        // Add inheritance relationships
        relationships.addAll(dsl.parents.map { it to child })
    }

    /** Builds the role hierarchy. */
    internal fun build(): List<Pair<Role, Role>> = relationships
}

/** DSL for defining a role. */
@AuthDsl
class RoleDefinitionDsl(val role: Role) {
    internal val parents = mutableListOf<Role>()

    /**
     * Specifies parent roles that this role inherits from.
     *
     * @param parentRoles The parent roles
     */
    fun inheritsFrom(vararg parentRoles: Role) {
        parents.addAll(parentRoles)
    }
}

/** Top-level function to define role hierarchy using the DSL. */
fun defineRoleHierarchy(block: RoleHierarchyDsl.() -> Unit): List<Pair<Role, Role>> {
    val dsl = RoleHierarchyDsl()
    dsl.block()
    return dsl.build()
}

/** Applies a role hierarchy configuration to a GraphRoleHierarchy. */
fun GraphRoleHierarchy.applyHierarchy(config: List<Pair<Role, Role>>) {
    for ((parent, child) in config) {
        addParentChildRelationship(parent, child)
    }
}
