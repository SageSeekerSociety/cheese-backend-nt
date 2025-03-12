package org.rucca.cheese.auth.registry

import org.rucca.cheese.auth.core.Action
import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.ResourceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for registering actions and resources. Provides a convenient way to register multiple
 * actions and resources for a domain.
 */
@Service
class RegistrationService(
    private val actionRegistry: ActionRegistry,
    private val resourceRegistry: ResourceRegistry,
) {
    private val logger = LoggerFactory.getLogger(RegistrationService::class.java)

    /**
     * Registers all actions from an enum class for a domain.
     *
     * @param actions Array of actions to register
     */
    fun registerActions(vararg actions: Action) {
        actions.forEach { action -> actionRegistry.registerAction(action) }

        if (actions.isNotEmpty()) {
            logger.info("Registered ${actions.size} actions for domain ${actions[0].domain.name}")
        }
    }

    /**
     * Registers all resources from an enum class for a domain.
     *
     * @param resources Array of resources to register
     */
    fun registerResources(vararg resources: ResourceType) {
        resources.forEach { resource -> resourceRegistry.registerResource(resource) }

        if (resources.isNotEmpty()) {
            logger.info(
                "Registered ${resources.size} resources for domain ${resources[0].domain.name}"
            )
        }
    }

    /**
     * Registers all enum values that implement Action or ResourceType. This method uses reflection
     * to find all enum values and register them.
     *
     * @param T The enum class to scan
     * @param domain The domain to associate with the actions and resources
     */
    fun <T : Enum<T>> registerEnumValues(enumClass: Class<T>, domain: Domain) {
        val enumValues = enumClass.enumConstants

        val actions = enumValues.filterIsInstance<Action>()
        val resources = enumValues.filterIsInstance<ResourceType>()

        registerActions(*actions.toTypedArray())
        registerResources(*resources.toTypedArray())

        logger.info(
            "Auto-registered ${actions.size} actions and ${resources.size} resources " +
                "from enum ${enumClass.simpleName} for domain ${domain.name}"
        )
    }
}

inline fun <reified T : Enum<T>> RegistrationService.registerEnumValues(domain: Domain) {
    registerEnumValues(T::class.java, domain)
}
