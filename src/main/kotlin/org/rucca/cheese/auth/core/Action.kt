package org.rucca.cheese.auth.core

/**
 * Represents an action that can be performed within a domain. Actions are typically verbs like
 * "create", "read", "update", etc.
 */
interface Action {
    /** Unique identifier of the action within its domain. */
    val actionId: String

    /** The domain this action belongs to. */
    val domain: Domain
}
