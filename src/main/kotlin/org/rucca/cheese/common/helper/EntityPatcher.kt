/**
 * EntityPatchService - A generic PATCH operation implementation for JPA entities
 *
 * This service provides a clean, type-safe way to update entity properties from DTOs with a single
 * database operation while maintaining proper validation and business logic.
 *
 * Features:
 * - Type-safe property updates using Kotlin property references
 * - Protection of system fields and private properties
 * - Single database update regardless of how many fields are modified
 * - DSL-style API for custom field handling
 *
 * Usage:
 * ```kotlin
 * // In your service class
 * @Transactional
 * fun patchEntity(id: Long, patchDto: PatchDTO): Entity {
 *   // 1. Load entity and perform validations
 *   val entity = repository.findById(id).orElseThrow()
 *
 *   // 2. Validate fields that need business rule checks
 *   if (patchDto.name != null && patchDto.name != entity.name) {
 *     ensureNameNotExists(patchDto.name)
 *   }
 *
 *   // 3. Apply patch with custom handlers as needed
 *   entityPatchService.patch(entity, patchDto) {
 *     // Simple property update
 *     handle(Entity::name) { e, value -> e.name = value }
 *
 *     // Custom handling for special cases
 *     handle(Entity::status) { e, value ->
 *       e.status = value
 *       e.statusChangedAt = LocalDateTime.now()
 *     }
 *   }
 *
 *   // 4. Save and return
 *   return repository.save(entity)
 * }
 * ```
 *
 * Note on using with JPA/Hibernate: All database operations (validations, related entity lookups)
 * should be done BEFORE applying the patch to avoid Hibernate's flush behavior causing multiple
 * updates.
 */
package org.rucca.cheese.common.helper

import jakarta.persistence.Id
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.springframework.stereotype.Component

/** Marks a private field that should be updatable via PATCH operations */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Patchable

/** Field handler function type */
typealias FieldHandler<T> = (T, Any) -> Unit

/** Patch handler DSL for configuring custom field handlers */
class PatchHandlerDSL<T : Any, P> {
    private val handlers = mutableMapOf<String, FieldHandler<T>>()

    /**
     * Defines a type-safe handler for a specific field
     *
     * @param property The property in the PATCH DTO class to handle
     * @param handler The handler function for the field
     */
    fun <V : Any> handle(property: KProperty1<P, V?>, handler: (entity: T, value: V) -> Unit) {
        handlers[property.name] = { entity, anyValue ->
            try {
                @Suppress("UNCHECKED_CAST") val value = anyValue as V
                handler(entity, value)
            } catch (e: ClassCastException) {
                throw IllegalArgumentException(
                    "Type mismatch: Field '${property.name}' expects type ${property.returnType}, " +
                        "but received type ${anyValue::class.simpleName}",
                    e,
                )
            }
        }
    }

    /** Get all registered handlers */
    internal fun getHandlers(): Map<String, FieldHandler<T>> = handlers
}

/** Generic entity patch service for handling PATCH operations */
@Component
class EntityPatcher {
    // Default protected fields that shouldn't be modified
    private val defaultProtectedFields = setOf("id", "createdAt", "updatedAt", "deletedAt")

    /**
     * Apply a patch to an entity using DSL-style configuration
     *
     * @param entity The entity to update
     * @param patchData The DTO or Map containing update data
     * @param additionalProtectedFields Additional fields to protect from updates
     * @param configure DSL configuration block for custom field handling
     * @return The updated entity (not yet persisted)
     */
    fun <T : Any, P : Any> patch(
        entity: T,
        patchData: P,
        additionalProtectedFields: Set<String> = emptySet(),
        configure: PatchHandlerDSL<T, P>.() -> Unit = {},
    ): T {
        val dsl = PatchHandlerDSL<T, P>().apply(configure)

        // Get all protected fields
        val protectedFields = defaultProtectedFields + additionalProtectedFields

        // Apply patch and return updated entity (caller is responsible for saving)
        return applyPatch(entity, patchData.convertToMap(), dsl.getHandlers(), protectedFields)
    }

    /** Applies patch to an entity */
    private fun <T : Any> applyPatch(
        entity: T,
        patchMap: Map<String, Any?>,
        handlers: Map<String, FieldHandler<T>>,
        protectedFields: Set<String>,
    ): T {
        val entityClass = entity::class

        // Get all modifiable properties, filtering out protected fields
        val properties =
            entityClass.memberProperties.filterIsInstance<KMutableProperty1<T, Any?>>().filter {
                property ->
                when {
                    // Skip protected fields
                    property.name in protectedFields -> false
                    // Skip fields with @Id annotation
                    property.findAnnotation<Id>() != null -> false
                    // Skip private fields without @Patchable annotation
                    property.visibility == KVisibility.PRIVATE &&
                        property.findAnnotation<Patchable>() == null -> false
                    // Allow modification by default
                    else -> true
                }
            }

        // Process each non-null field
        patchMap.forEach { (fieldName, value) ->
            if (value != null) {
                // Prioritize custom handlers
                if (handlers.containsKey(fieldName)) {
                    handlers[fieldName]?.invoke(entity, value)
                } else {
                    // Find matching property
                    val property = properties.find { it.name == fieldName }
                    if (property != null) {
                        property.isAccessible = true
                        try {
                            // Set property value directly without type conversion
                            property.set(entity, value)
                        } catch (e: ClassCastException) {
                            throw IllegalArgumentException(
                                "Type mismatch: Field '$fieldName' expects type ${property.returnType}, " +
                                    "but received type ${value::class.simpleName}. Ensure DTO and Entity " +
                                    "field types match, or use a custom handler.",
                                e,
                            )
                        } catch (e: Exception) {
                            throw IllegalArgumentException(
                                "Error setting field '$fieldName': ${e.message}",
                                e,
                            )
                        }
                    }
                }
            }
        }

        return entity
    }

    /** Converts any object to a Map, preserving non-null values */
    private fun Any.convertToMap(): Map<String, Any?> {
        return when (this) {
            is Map<*, *> -> this.filterKeys { it is String }.mapKeys { it.key as String }
            else -> {
                this::class
                    .memberProperties
                    .associate { prop -> prop.name to prop.getter.call(this) }
                    .filterValues { it != null }
            }
        }
    }
}
