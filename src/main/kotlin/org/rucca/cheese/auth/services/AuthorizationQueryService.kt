package org.rucca.cheese.auth.services

import org.rucca.cheese.auth.context.PermissionContextProviderFactory
import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.ResourceType
import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.domain.DomainRoleProviderRegistry
import org.rucca.cheese.auth.hierarchy.RoleHierarchy
import org.rucca.cheese.auth.spring.UserSecurityService
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

interface AuthorizationQueryService {
    /**
     * Gets all effective roles (System + Domain specific) for a user regarding a specific resource.
     */
    fun getEffectiveRoles(
        userId: IdType,
        domain: Domain,
        resourceType: ResourceType,
        resourceId: IdType?,
    ): Set<Role>

    /** Checks if a user effectively has a specific role for a resource (considering hierarchy). */
    fun hasEffectiveRole(
        userId: IdType,
        roleToCheck: Role,
        domain: Domain,
        resourceType: ResourceType,
        resourceId: IdType?,
    ): Boolean
}

@Service
class DefaultAuthorizationQueryService(
    private val userSecurityService: UserSecurityService,
    private val domainRoleProviderRegistry: DomainRoleProviderRegistry,
    private val contextProviderFactory: PermissionContextProviderFactory,
    private val roleHierarchy: RoleHierarchy,
) : AuthorizationQueryService {

    override fun getEffectiveRoles(
        userId: IdType,
        domain: Domain,
        resourceType: ResourceType,
        resourceId: IdType?,
    ): Set<Role> {
        val systemRoles = userSecurityService.getUserRoles(userId)
        var domainRoles = emptySet<Role>()

        val contextProvider = contextProviderFactory.getProvider(domain.name)
        val domainProvider = domainRoleProviderRegistry.getProvider(domain.name)

        if (contextProvider != null && domainProvider != null && resourceId != null) {
            // Build context specific to the resource
            val context = contextProvider.getContext(resourceType.typeName, resourceId)
            domainRoles = domainProvider.getRoles(userId, context)
        }
        // Return the combined set of roles
        return systemRoles + domainRoles
    }

    override fun hasEffectiveRole(
        userId: IdType,
        roleToCheck: Role,
        domain: Domain,
        resourceType: ResourceType,
        resourceId: IdType?,
    ): Boolean {
        val effectiveRoles = getEffectiveRoles(userId, domain, resourceType, resourceId)
        return effectiveRoles.contains(roleToCheck) ||
            effectiveRoles.any { roleHierarchy.isParentRole(roleToCheck, it) }
    }
}
