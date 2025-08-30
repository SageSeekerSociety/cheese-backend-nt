package org.rucca.cheese.auth.core

/**
 * Represents a business domain within the application. Domains provide a way to organize and
 * isolate permissions.
 */
interface Domain {
    /** Unique name of the domain. */
    val name: String
}
