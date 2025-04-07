package org.rucca.cheese.notification.auth

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.PermissionConfigurationService
import org.rucca.cheese.auth.core.applyConfiguration
import org.rucca.cheese.auth.domain.DomainPermissionService
import org.rucca.cheese.auth.dsl.definePermissions
import org.rucca.cheese.auth.registry.RegistrationService
import org.springframework.stereotype.Component

@Component
class NotificationPermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) : DomainPermissionService {

    override val domain: Domain = NotificationDomain

    @PostConstruct
    override fun configurePermissions() {
        registrationService.registerActions(*NotificationAction.entries.toTypedArray())
        registrationService.registerResources(*NotificationResource.entries.toTypedArray())

        // Define permissions: OWNER can do everything on NOTIFICATION resource
        val config = definePermissions {
            role(NotificationRole.OWNER) {
                can(
                        NotificationAction.LIST,
                        NotificationAction.VIEW,
                        NotificationAction.UPDATE,
                        NotificationAction.DELETE,
                    )
                    .on(NotificationResource.NOTIFICATION)
                    .all()
            }
        }

        permissionService.applyConfiguration(config)
    }
}
