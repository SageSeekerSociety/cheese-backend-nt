package org.rucca.cheese.auth.core

/**
 * Represents a permission which combines an action and a resource type. Permissions define what
 * actions can be performed on which resources.
 *
 * @param A The type of action
 * @param R The type of resource
 * @property action The action part of this permission
 * @property resourceType The resource part of this permission
 */
data class Permission<A : Action, R : ResourceType>(val action: A, val resourceType: R) {
    /**
     * Returns a string representation of this permission in the format "domain:action:resource".
     */
    override fun toString(): String =
        "${action.domain.name}:${action.actionId}:${resourceType.typeName}"
}
