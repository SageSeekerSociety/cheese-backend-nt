package org.rucca.cheese.auth.core

/**
 * Represents a resource type within a domain. Resources are typically nouns like "order",
 * "product", "user", etc.
 */
interface ResourceType {
    /** Unique name of the resource within its domain. */
    val typeName: String

    /** The domain this resource belongs to. */
    val domain: Domain
}
