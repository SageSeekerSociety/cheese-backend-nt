package org.rucca.cheese.auth.core

import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.common.persistent.IdType

/**
 * Represents a rule that determines whether a permission is granted. Rules can include conditions
 * like ownership check, time constraints, etc.
 *
 * @param A The type of action
 * @param R The type of resource
 */
class PermissionRule<A : Action, R : ResourceType> {
    private val conditions = mutableListOf<PermissionCondition<A, R>>()

    /**
     * Adds a condition to this rule.
     *
     * @param condition The condition to add
     */
    fun withCondition(condition: PermissionCondition<A, R>) {
        conditions.add(condition)
    }

    /**
     * Adds a condition that checks if the user is the owner of the resource. Requires an
     * "ownerIdProvider" function in the context.
     */
    @Suppress("UNCHECKED_CAST")
    fun ownerOnly() {
        withCondition { userId, _, _, resourceId, context ->
            if (resourceId == null) {
                false
            } else {
                val ownerIdProvider = context["ownerIdProvider"] as? (IdType) -> IdType
                ownerIdProvider?.invoke(resourceId) == userId.userId
            }
        }
    }

    /**
     * Evaluates all conditions to determine if the permission is granted. Returns true if there are
     * no conditions or if all conditions pass.
     *
     * @param userInfo The user information
     * @param action The action being evaluated
     * @param resourceType The resource type being evaluated
     * @param resourceId Optional ID of a specific resource instance
     * @param context Additional context for evaluation
     * @return true if the permission is granted, false otherwise
     */
    internal fun evaluate(
        userInfo: AuthUserInfo,
        action: A,
        resourceType: R,
        resourceId: IdType?,
        context: Map<String, Any>,
    ): Boolean {
        return conditions.isEmpty() ||
            conditions.all { condition ->
                condition.invoke(userInfo, action, resourceType, resourceId, context)
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
