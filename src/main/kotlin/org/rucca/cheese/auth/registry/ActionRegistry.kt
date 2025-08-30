package org.rucca.cheese.auth.registry

import org.rucca.cheese.auth.core.Action
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** Registry for all actions in the application. Actions are registered by domain and name. */
@Component
class ActionRegistry {
    private val logger = LoggerFactory.getLogger(ActionRegistry::class.java)
    private val actions = mutableMapOf<Pair<String, String>, Action>()

    /**
     * Registers an action.
     *
     * @param action The action to register
     * @throws IllegalArgumentException if an action with the same domain and name already exists
     */
    fun <A : Action> registerAction(action: A) {
        val key = action.domain.name to action.actionId

        if (actions.containsKey(key)) {
            throw IllegalArgumentException(
                "Action ${action.actionId} already registered for domain ${action.domain.name}"
            )
        }

        logger.debug("Registering action: ${action.domain.name}:${action.actionId}")
        actions[key] = action
    }

    /**
     * Gets an action by domain name and action name.
     *
     * @param domainName The domain name
     * @param actionName The action name
     * @return The action
     * @throws IllegalArgumentException if the action is not found
     */
    fun getAction(domainName: String, actionName: String): Action {
        val key = domainName to actionName
        return actions[key]
            ?: throw IllegalArgumentException("Action not found: $domainName:$actionName")
    }

    /**
     * Gets all actions for a domain.
     *
     * @param domainName The domain name
     * @return List of actions
     */
    fun getActionsForDomain(domainName: String): List<Action> {
        return actions.filter { (key, _) -> key.first == domainName }.values.toList()
    }

    /**
     * Gets all registered actions.
     *
     * @return All actions
     */
    fun getAllActions(): Collection<Action> {
        return actions.values
    }
}
