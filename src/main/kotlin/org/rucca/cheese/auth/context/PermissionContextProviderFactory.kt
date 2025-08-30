package org.rucca.cheese.auth.context

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * Factory for permission context providers. Discovers and manages all PermissionContextProvider
 * implementations.
 */
@Component
class PermissionContextProviderFactory(private val applicationContext: ApplicationContext) {
    private val logger = LoggerFactory.getLogger(PermissionContextProviderFactory::class.java)
    private val providers = mutableMapOf<String, PermissionContextProvider>()

    /** Initialize the factory by discovering all context providers. */
    @PostConstruct
    fun initialize() {
        logger.info("Initializing permission context provider factory")

        // Get all context providers from the application context
        val providersMap = applicationContext.getBeansOfType(PermissionContextProvider::class.java)

        // Register each provider by its domain name
        for ((_, provider) in providersMap) {
            val domainName = provider.domain.name
            providers[domainName] = provider
            logger.info("Registered permission context provider for domain: $domainName")
        }
    }

    /**
     * Get a context provider for a domain.
     *
     * @param domainName The domain name
     * @return The context provider, or null if not found
     */
    fun getProvider(domainName: String): PermissionContextProvider? {
        return providers[domainName]
    }
}
