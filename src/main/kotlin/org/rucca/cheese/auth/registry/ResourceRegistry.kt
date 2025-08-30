package org.rucca.cheese.auth.registry

import org.rucca.cheese.auth.core.ResourceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Registry for all resource types in the application. Resources are registered by domain and name.
 */
@Component
class ResourceRegistry {
    private val logger = LoggerFactory.getLogger(ResourceRegistry::class.java)
    private val resources = mutableMapOf<Pair<String, String>, ResourceType>()

    /**
     * Registers a resource type.
     *
     * @param resource The resource type to register
     * @throws IllegalArgumentException if a resource with the same domain and name already exists
     */
    fun <R : ResourceType> registerResource(resource: R) {
        val key = resource.domain.name to resource.typeName

        if (resources.containsKey(key)) {
            throw IllegalArgumentException(
                "Resource ${resource.typeName} already registered for domain ${resource.domain.name}"
            )
        }

        logger.debug("Registering resource: ${resource.domain.name}:${resource.typeName}")
        resources[key] = resource
    }

    /**
     * Gets a resource type by domain name and resource name.
     *
     * @param domainName The domain name
     * @param resourceName The resource name
     * @return The resource type
     * @throws IllegalArgumentException if the resource is not found
     */
    fun getResource(domainName: String, resourceName: String): ResourceType {
        val key = domainName to resourceName
        return resources[key]
            ?: throw IllegalArgumentException("Resource not found: $domainName:$resourceName")
    }

    /**
     * Gets all resource types for a domain.
     *
     * @param domainName The domain name
     * @return List of resource types
     */
    fun getResourcesForDomain(domainName: String): List<ResourceType> {
        return resources.filter { (key, _) -> key.first == domainName }.values.toList()
    }

    /**
     * Gets all registered resource types.
     *
     * @return All resource types
     */
    fun getAllResources(): Collection<ResourceType> {
        return resources.values
    }
}
