package org.rucca.cheese.auth.core

import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory

/**
 * Represents a rule that determines whether a permission is granted. Rules can include conditions
 * like ownership check, time constraints, etc.
 *
 * @param A The type of action
 * @param R The type of resource
 */
class PermissionRule<A : Action, R : ResourceType> {

    private val logger = LoggerFactory.getLogger(PermissionRule::class.java)

    internal data class ConditionEntry<A : Action, R : ResourceType>(
        val name: String?, // Optional name for the condition
        val condition: PermissionCondition<A, R>, // The actual condition logic
    )

    private val conditions = mutableListOf<ConditionEntry<A, R>>()

    /**
     * Adds an anonymous condition to this rule.
     *
     * @param condition The condition lambda to add.
     */
    fun withCondition(condition: PermissionCondition<A, R>) {
        // Add with a null name
        conditions.add(ConditionEntry(name = null, condition = condition))
    }

    /**
     * Adds a named condition to this rule.
     *
     * @param name A descriptive name for the condition (used for logging/debugging).
     * @param condition The condition lambda to add.
     */
    fun withCondition(name: String, condition: PermissionCondition<A, R>) {
        conditions.add(ConditionEntry(name = name, condition = condition))
    }

    fun orCondition(vararg conditionsToOr: PermissionCondition<A, R>) {
        orCondition(null, *conditionsToOr)
    }

    /**
     * Adds a combined condition that passes if *at least one* of the provided inner conditions
     * evaluates to true (OR logic). This entire OR block is treated as a single condition within
     * the rule's overall AND logic.
     *
     * @param name An optional descriptive name for this combined OR condition block.
     * @param conditionsToOr One or more condition lambdas to be evaluated with OR logic. It's
     *   recommended to provide at least two for OR logic to be meaningful.
     */
    fun orCondition(name: String? = null, vararg conditionsToOr: PermissionCondition<A, R>) {
        // While technically works with one, OR logic implies multiple options.
        if (conditionsToOr.size < 2) {
            logger.warn(
                "Using orCondition with less than two inner conditions ({}) might not be logical.",
                name ?: "anonymous OR",
            )
            // You could choose to throw an error here if desired:
            // require(conditionsToOr.size >= 2) { "orCondition requires at least two condition
            // lambdas." }
        }
        if (conditionsToOr.isEmpty()) {
            logger.warn("Ignoring empty orCondition group: {}", name ?: "anonymous OR")
            return // Don't add an empty OR group
        }

        // Create the combined OR condition lambda
        val combinedOrLambda: PermissionCondition<A, R> =
            { userInfo, action, resourceType, resourceId, context ->
                val orGroupName = name ?: "anonymous OR"
                // Use 'any' to check if at least one inner condition passes
                val result =
                    conditionsToOr.any { innerCondition ->
                        try {
                            // Evaluate the inner condition
                            val innerResult =
                                innerCondition.invoke(
                                    userInfo,
                                    action,
                                    resourceType,
                                    resourceId,
                                    context,
                                )
                            // Optionally log inner condition results if needed for complex
                            // debugging
                            // logger.trace("Inner condition of OR group '{}' result: {}",
                            // orGroupName, innerResult)
                            innerResult // Return the result of the inner condition
                        } catch (e: Exception) {
                            // Log error from inner condition, treat it as false for the 'any' check
                            logger.error(
                                "Error evaluating inner condition within OR group '{}': {}",
                                orGroupName,
                                e.message,
                                e,
                            )
                            false // Fail the inner condition if it throws an exception
                        }
                    }
                // Log the final result of the OR group evaluation
                logger.trace("OR condition group '{}' evaluated to: {}", orGroupName, result)
                result
            }

        // Add the combined OR lambda as a single ConditionEntry to the main list.
        // This entry must pass along with any other direct conditions added via withCondition.
        this.conditions.add(ConditionEntry(name = name, condition = combinedOrLambda))
    }

    /**
     * Adds a common condition that checks if the user is the owner of the resource. Requires an
     * "ownerIdProvider" function in the context, which takes the resourceId and returns the owner's
     * IdType.
     */
    @Suppress("UNCHECKED_CAST")
    fun ownerOnly() {
        // Use the named version for built-in conditions for clarity
        withCondition("ownerOnly") { userInfo, _, _, resourceId, context ->
            if (resourceId == null) {
                logger.trace("Condition 'ownerOnly' failed: resourceId is null")
                false // Cannot check ownership without a resource ID
            } else {
                // Attempt to get the provider function from context
                val ownerIdProvider =
                    context["ownerIdProvider"] as? (IdType) -> IdType? // Allow nullable return
                if (ownerIdProvider == null) {
                    logger.warn(
                        "Condition 'ownerOnly' failed: 'ownerIdProvider' function not found in context"
                    )
                    false // Cannot determine owner without the provider
                } else {
                    val ownerId = ownerIdProvider.invoke(resourceId)
                    val isOwner = ownerId != null && ownerId == userInfo.userId
                    logger.trace(
                        "Condition 'ownerOnly' check: userId={}, resourceId={}, ownerId={}, result={}",
                        userInfo.userId,
                        resourceId,
                        ownerId,
                        isOwner,
                    )
                    isOwner // Check if ownerId exists and matches the user
                }
            }
        }
    }

    /**
     * Evaluates all conditions attached to this rule to determine if the permission is granted.
     * Returns true only if there are no conditions or if *all* conditions evaluate to true.
     * Evaluation stops on the first condition that returns false.
     *
     * @param userInfo The user information.
     * @param action The action being evaluated.
     * @param resourceType The resource type being evaluated.
     * @param resourceId Optional ID of a specific resource instance.
     * @param context Additional context for evaluation.
     * @return true if the permission is granted based on the conditions, false otherwise.
     */
    internal fun evaluate(
        userInfo: AuthUserInfo,
        action: A,
        resourceType: R,
        resourceId: IdType?,
        context: Map<String, Any>,
    ): Boolean {
        if (conditions.isEmpty()) {
            return true
        }

        return conditions.all { entry ->
            val conditionName = entry.name ?: "anonymous"
            logger.trace(
                "Evaluating condition: '{}' for user: {}, action: {}, resource: {}:{}",
                conditionName,
                userInfo.userId,
                action.actionId,
                resourceType.typeName,
                resourceId ?: "N/A",
            )
            val result =
                try {
                    entry.condition.invoke(userInfo, action, resourceType, resourceId, context)
                } catch (e: Exception) {
                    logger.error(
                        "Error evaluating permission condition '{}': {}",
                        conditionName,
                        e.message,
                        e,
                    )
                    false
                }
            if (!result) {
                logger.debug("Permission denied by condition: '{}'", conditionName)
            }
            result
        }
    }
}

/**
 * Type alias for a permission condition function. These functions determine if a permission is
 * granted based on various inputs.
 */
typealias PermissionCondition<A, R> =
    (
        userInfo: AuthUserInfo,
        action: A,
        resourceType: R,
        resourceId: IdType?,
        context: Map<String, Any>,
    ) -> Boolean
