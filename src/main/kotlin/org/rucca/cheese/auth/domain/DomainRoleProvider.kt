package org.rucca.cheese.auth.domain

import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.common.persistent.IdType

/**
 * Interface for domain-specific role providers. Each domain implements this to provide user roles
 * within the domain.
 */
interface DomainRoleProvider {
    /** The domain this provider serves. */
    val domain: Domain

    /**
     * Gets roles for a user within the domain context.
     *
     * @param userId The user ID
     * @param context Additional context with domain-specific identifiers
     * @return Set of domain-specific roles
     */
    fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role>
}
