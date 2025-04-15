package org.rucca.cheese.auth.spring

import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.domain.DomainRoleProviderRegistry
import org.rucca.cheese.auth.hierarchy.RoleHierarchy
import org.rucca.cheese.common.persistent.IdType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Spring configuration for the security system. */
@Configuration
class SecurityConfiguration {
    /** Creates the permission evaluator bean. */
    @Bean
    fun permissionEvaluator(
        permissionService: PermissionConfigurationService,
        roleHierarchy: RoleHierarchy,
        roleProviderRegistry: DomainRoleProviderRegistry,
    ): PermissionEvaluator {
        return DefaultPermissionEvaluator(permissionService, roleHierarchy, roleProviderRegistry)
    }
}

/** Web MVC configuration for security. */
@Configuration
class SecurityWebConfiguration(private val authUserArgumentResolver: AuthUserArgumentResolver) :
    WebMvcConfigurer {
    /** Adds custom argument resolvers. */
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserArgumentResolver)
    }
}

/** Service for getting user security information. */
interface UserSecurityService {
    /**
     * Gets the role for a user.
     *
     * @param userId The user ID
     * @return The user's role
     */
    fun getUserRoles(userId: IdType): Set<Role>
}
