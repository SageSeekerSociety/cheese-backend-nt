package org.rucca.cheese.auth.context

import org.rucca.cheese.auth.core.Domain
import org.rucca.cheese.auth.core.ResourceType
import org.rucca.cheese.common.persistent.IdType

/** Type-safe context builder DSL. */
@DslMarker annotation class ContextDsl

/** Context builder class for DSL. */
@ContextDsl
class ContextBuilder {
    private val context = mutableMapOf<String, Any>()

    /** Adds a value to the context using a typed key. */
    operator fun <T : Any> ContextKey<T>.invoke(value: T) {
        put(context, value)
    }

    /** Adds a value to the context only if the condition is true. */
    fun <T : Any> ContextKey<T>.ifTrue(condition: Boolean, value: T) {
        if (condition) put(context, value)
    }

    /** Adds a value to the context only if it's not null. */
    fun <T : Any> ContextKey<T>.ifNotNull(value: T?) {
        value?.let { put(context, it) }
    }

    /** Builds the context map. */
    fun build(): Map<String, Any> = context.toMap()
}

/** Creates a context using the DSL. */
fun buildContext(block: ContextBuilder.() -> Unit): Map<String, Any> {
    return ContextBuilder().apply(block).build()
}

/** Extension function to create a resource context for a specific domain. */
inline fun <T : Domain, R : ResourceType> buildResourceContext(
    domain: T,
    resourceType: R,
    resourceId: IdType?,
    crossinline block: ContextBuilder.() -> Unit,
): Map<String, Any> {
    return buildContext {
        // Add domain and resource info
        DomainContextKeys.DOMAIN_NAME(domain.name)
        DomainContextKeys.RESOURCE_TYPE(resourceType.typeName)
        DomainContextKeys.RESOURCE_ID.ifNotNull(resourceId)

        // Execute the custom block
        block()
    }
}
