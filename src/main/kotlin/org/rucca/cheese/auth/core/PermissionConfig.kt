package org.rucca.cheese.auth.core

/**
 * Configuration for a permission, combining an action, resource type, and rule.
 *
 * @param A The type of action
 * @param R The type of resource
 * @property action The action
 * @property resourceType The resource type
 * @property rule The permission rule
 */
data class PermissionConfig<A : Action, R : ResourceType>(
    val action: A,
    val resourceType: R,
    val rule: PermissionRule<A, R>,
)
