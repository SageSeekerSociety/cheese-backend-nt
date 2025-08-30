package org.rucca.cheese.auth.context

import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.common.persistent.IdType

/**
 * Interface for domain-specific permission context providers. Implementations provide context
 * information for permission evaluation.
 */
interface PermissionContextProvider {
    /** Get the domain this provider is for. */
    val domain: Domain

    /**
     * Get context information for permission evaluation.
     *
     * @param resourceId Optional resource ID for context-specific providers
     * @return Map of context information
     */
    fun getContext(resourceName: String, resourceId: IdType?): Map<String, Any>
}
