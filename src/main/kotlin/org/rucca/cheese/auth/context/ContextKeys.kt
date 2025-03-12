package org.rucca.cheese.auth.context

import org.rucca.cheese.common.persistent.IdType

/**
 * Type-safe context key. Provides type-safe access to context values while preserving the Map
 * structure.
 *
 * @param T The type of the value associated with this key
 * @param name The string name of the key
 */
class ContextKey<T : Any>(val name: String) {
    /** Gets the typed value from a context map. */
    @Suppress("UNCHECKED_CAST") fun get(context: Map<String, Any>): T? = context[name] as? T

    /** Puts a typed value into a context map. */
    fun put(context: MutableMap<String, Any>, value: T) {
        context[name] = value as Any
    }
}

object DomainContextKeys {
    val DOMAIN_NAME = ContextKey<String>("domainName")
    val RESOURCE_TYPE = ContextKey<String>("resourceType")
    val RESOURCE_ID = ContextKey<IdType>("resourceId")
}
