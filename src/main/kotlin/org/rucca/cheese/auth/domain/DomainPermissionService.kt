package org.rucca.cheese.auth.domain

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.core.Domain

/**
 * Interface for domain-specific permission configuration services. Each domain should provide an
 * implementation of this interface to configure its permissions.
 */
interface DomainPermissionService {
    /** The domain this service configures permissions for. */
    val domain: Domain

    /**
     * Configures permissions for the domain. This method is called automatically after the service
     * is initialized.
     */
    @PostConstruct fun configurePermissions()
}
