package org.rucca.cheese.auth.domain

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.core.Domain
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * Registry for all domains in the application. Automatically discovers and registers domains from
 * DomainPermissionServices.
 */
@Component
class DomainRegistry(private val applicationContext: ApplicationContext) {
    private val logger = LoggerFactory.getLogger(DomainRegistry::class.java)
    private val domains = mutableMapOf<String, Domain>()

    /** Initializes the registry by discovering all domains from DomainPermissionServices. */
    @PostConstruct
    fun initialize() {
        logger.info("Initializing domain registry")

        // Get all domain permission services
        val domainServices =
            applicationContext.getBeansOfType(DomainPermissionService::class.java).values

        // Register domains from services
        domainServices.forEach { service ->
            val domain = service.domain
            logger.info("Registering domain: ${domain.name}")
            registerDomain(domain)
        }
    }

    /**
     * Registers a domain.
     *
     * @param domain The domain to register
     * @throws IllegalArgumentException if a domain with the same name already exists
     */
    fun registerDomain(domain: Domain) {
        if (domains.containsKey(domain.name)) {
            throw IllegalArgumentException("Domain with name ${domain.name} already registered")
        }

        domains[domain.name] = domain
    }

    /**
     * Gets a domain by name.
     *
     * @param name The domain name
     * @return The domain, or null if not found
     */
    fun getDomain(name: String): Domain? {
        return domains[name]
    }

    /**
     * Gets all registered domains.
     *
     * @return All domains
     */
    fun getAllDomains(): Collection<Domain> {
        return domains.values
    }
}
