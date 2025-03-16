package org.rucca.cheese.auth.domain

import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * Registry for domain role providers. Automatically discovers and manages all DomainRoleProvider
 * implementations.
 */
@Component
class DomainRoleProviderRegistry(private val applicationContext: ApplicationContext) {
    private val logger = LoggerFactory.getLogger(DomainRoleProviderRegistry::class.java)
    private val providers = mutableMapOf<String, DomainRoleProvider>()

    /** Initializes the registry by discovering all domain role providers. */
    @PostConstruct
    fun initialize() {
        logger.info("Initializing domain role provider registry")

        // Get all domain role providers
        val roleProviders = applicationContext.getBeansOfType(DomainRoleProvider::class.java).values

        // Register providers
        roleProviders.forEach { provider ->
            val domainName = provider.domain.name
            logger.info("Registering domain role provider for domain: $domainName")
            registerProvider(provider)
        }
    }

    /**
     * Registers a domain role provider.
     *
     * @param provider The provider to register
     * @throws IllegalArgumentException if a provider for the same domain already exists
     */
    fun registerProvider(provider: DomainRoleProvider) {
        val domainName = provider.domain.name

        if (providers.containsKey(domainName)) {
            throw IllegalArgumentException(
                "Role provider for domain $domainName already registered"
            )
        }

        providers[domainName] = provider
    }

    /**
     * Gets a domain role provider by domain name.
     *
     * @param domainName The domain name
     * @return The domain role provider, or null if not found
     */
    fun getProvider(domainName: String): DomainRoleProvider? {
        return providers[domainName]
    }

    /**
     * Gets all registered domain role providers.
     *
     * @return All providers
     */
    fun getAllProviders(): Collection<DomainRoleProvider> {
        return providers.values
    }
}
