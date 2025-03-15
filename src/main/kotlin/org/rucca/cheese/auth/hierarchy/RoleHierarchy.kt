package org.rucca.cheese.auth.hierarchy

import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.core.toDomainRoleId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Interface for role hierarchy management. Role hierarchy allows roles to inherit permissions from
 * parent roles.
 */
interface RoleHierarchy {
    /**
     * Gets all parent roles for a role, including indirect parents.
     *
     * @param role The role to get parents for
     * @return Set of parent roles
     */
    fun getAllParentRoles(role: Role): Set<Role>

    /**
     * Checks if a role is a parent of another role.
     *
     * @param parent The potential parent role
     * @param child The potential child role
     * @return true if parent is a direct or indirect parent of child
     */
    fun isParentRole(parent: Role, child: Role): Boolean
}

/**
 * Implementation of RoleHierarchy using a graph structure. Supports multiple inheritance (a role
 * can have multiple parent roles).
 */
@Component
class GraphRoleHierarchy : RoleHierarchy {
    private val logger = LoggerFactory.getLogger(GraphRoleHierarchy::class.java)

    // Stores the direct parent roles for each role
    private val directParents = mutableMapOf<Role, MutableSet<Role>>()

    // Cache for getAllParentRoles to avoid recalculating
    private val allParentsCache = mutableMapOf<Role, Set<Role>>()

    /**
     * Adds a parent-child relationship between roles.
     *
     * @param parent The parent role
     * @param child The child role
     * @throws IllegalArgumentException if the relationship would create a cycle
     */
    fun addParentChildRelationship(parent: Role, child: Role) {
        if (parent == child) {
            throw IllegalArgumentException(
                "Role cannot be its own parent: ${parent.toDomainRoleId()}"
            )
        }

        // Check for cycles
        if (isParentRole(child, parent)) {
            throw IllegalArgumentException(
                "Adding this relationship would create a cycle: ${parent.toDomainRoleId()} -> ${child.toDomainRoleId()}"
            )
        }

        // Initialize the set if needed
        if (!directParents.containsKey(child)) {
            directParents[child] = mutableSetOf()
        }

        // Add the relationship
        directParents[child]?.add(parent)

        // Clear the cache since the hierarchy has changed
        allParentsCache.clear()

        logger.debug(
            "Added role hierarchy: ${parent.toDomainRoleId()} is parent of ${child.toDomainRoleId()}"
        )
    }

    /**
     * Gets all parent roles for a role, including indirect parents.
     *
     * @param role The role to get parents for
     * @return Set of parent roles
     */
    override fun getAllParentRoles(role: Role): Set<Role> {
        // Try to get from cache
        allParentsCache[role]?.let {
            return it
        }

        val allParents = mutableSetOf<Role>()

        // Add direct parents
        val directParentsForRole = directParents[role] ?: emptySet()
        allParents.addAll(directParentsForRole)

        // Add parent's parents recursively
        for (parent in directParentsForRole) {
            allParents.addAll(getAllParentRoles(parent))
        }

        // Cache the result
        allParentsCache[role] = allParents

        return allParents
    }

    /**
     * Checks if a role is a parent of another role.
     *
     * @param parent The potential parent role
     * @param child The potential child role
     * @return true if parent is a direct or indirect parent of child
     */
    override fun isParentRole(parent: Role, child: Role): Boolean {
        return getAllParentRoles(child).contains(parent)
    }
}
