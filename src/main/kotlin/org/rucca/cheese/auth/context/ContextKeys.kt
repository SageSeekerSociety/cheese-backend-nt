package org.rucca.cheese.auth.context

import kotlin.reflect.KClass
import org.rucca.cheese.common.persistent.IdType

/**
 * Type-safe context key. Provides type-safe access to context values while preserving the Map
 * structure.
 *
 * @param T The type of the value associated with this key
 * @param name The string name of the key
 */
class ContextKey<T : Any>(val name: String, private val clazz: KClass<T>) {
    /** Gets the typed value from a context map. */
    @Suppress("UNCHECKED_CAST")
    fun get(context: Map<String, Any>): T? {
        val value = context[name]
        return if (clazz.isInstance(value)) value as? T else null
    }

    /** Puts a typed value into a context map. */
    fun put(context: MutableMap<String, Any>, value: T) {
        context[name] = value as Any
    }

    companion object {
        inline fun <reified T : Any> of(name: String) = ContextKey<T>(name, T::class)
    }
}

object DomainContextKeys {
    val DOMAIN_NAME = ContextKey.of<String>("domainName")
    val RESOURCE_TYPE = ContextKey.of<String>("resourceType")
    val RESOURCE_ID = ContextKey.of<IdType>("resourceId")
}
