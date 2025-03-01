package org.rucca.cheese.common.helper

import jakarta.persistence.Id
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.springframework.stereotype.Component

/**
 * Annotation to explicitly mark a field as patchable Use this on properties that should be
 * modifiable via PATCH operations
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Patchable

/**
 * Type conversion utility Provides conversion functionality for basic types and collection types
 */
object TypeConverter {
    /**
     * Converts a value to the specified Kotlin type
     *
     * @param value The value to convert
     * @param type Target Kotlin type
     * @return Converted value or null
     */
    @Suppress("UNCHECKED_CAST")
    fun convert(value: Any?, type: KType): Any? {
        if (value == null) return null

        val classifier = type.classifier as? KClass<*> ?: return value

        // Return if value is already the target type
        if (classifier.java.isAssignableFrom(value::class.java)) {
            return value
        }

        return when (classifier) {
            // Basic type conversions
            String::class -> value.toString()
            Int::class,
            Integer::class -> convertToInt(value)
            Long::class,
            java.lang.Long::class -> convertToLong(value)
            Double::class,
            java.lang.Double::class -> convertToDouble(value)
            Float::class,
            java.lang.Float::class -> convertToFloat(value)
            Boolean::class,
            java.lang.Boolean::class -> convertToBoolean(value)

            // Collection type conversions
            List::class -> convertToList(value, type)
            Set::class -> convertToSet(value, type)
            Map::class -> convertToMap(value, type)

            // Default: return original value
            else -> value
        }
    }

    /** Converts a value to Int */
    fun convertToInt(value: Any): Any {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: value
            else -> value
        }
    }

    /** Converts a value to Long */
    fun convertToLong(value: Any): Any {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: value
            else -> value
        }
    }

    /** Converts a value to Double */
    fun convertToDouble(value: Any): Any {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: value
            else -> value
        }
    }

    /** Converts a value to Float */
    fun convertToFloat(value: Any): Any {
        return when (value) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: value
            else -> value
        }
    }

    /** Converts a value to Boolean */
    fun convertToBoolean(value: Any): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.lowercase(Locale.getDefault()) == "true" || value == "1"
            is Number -> value.toInt() != 0
            else -> false
        }
    }

    /** Converts a value to List, considering the element type */
    fun convertToList(value: Any, type: KType): List<*> {
        if (value !is Collection<*>) return listOf(value)

        // Try to get element type
        val elementType = type.arguments.firstOrNull()?.type
        if (elementType != null) {
            // Convert each element to the target type
            return value.map { element ->
                if (element != null) convert(element, elementType) else null
            }
        }

        // Return as is if element type is unknown
        return value.toList()
    }

    /** Converts a value to Set, considering the element type */
    internal fun convertToSet(value: Any, type: KType): Set<*> {
        if (value !is Collection<*>) return setOf(value)

        // Try to get element type
        val elementType = type.arguments.firstOrNull()?.type
        if (elementType != null) {
            // Convert each element to the target type
            return value
                .map { element -> if (element != null) convert(element, elementType) else null }
                .toSet()
        }

        // Return as is if element type is unknown
        return value.toSet()
    }

    /** Converts a value to Map, considering key and value types */
    internal fun convertToMap(value: Any, type: KType): Map<*, *> {
        if (value !is Map<*, *>)
            throw IllegalArgumentException("Cannot convert ${value::class.simpleName} to Map")

        // Try to get key and value types
        val keyType = type.arguments.getOrNull(0)?.type
        val valueType = type.arguments.getOrNull(1)?.type

        if (keyType != null && valueType != null) {
            return value.entries.associate { (k, v) ->
                val convertedKey = if (k != null) convert(k, keyType) else null
                val convertedValue = if (v != null) convert(v, valueType) else null
                convertedKey to convertedValue
            }
        }

        // Return as is if key or value type is unknown
        return value
    }

    /**
     * Generic method to convert a value to a specified type
     *
     * @param value The value to convert
     * @return Converted value of type V
     * @throws IllegalArgumentException if conversion is not possible
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified V> convertTo(value: Any?): V {
        if (value == null) {
            // If V is nullable, return null, otherwise throw exception
            if (null is V) return null as V
            throw IllegalArgumentException(
                "Cannot convert null to non-nullable ${V::class.simpleName}"
            )
        }

        // Return if value is already the target type
        if (value is V) return value

        return when (V::class) {
            // Basic type conversions
            String::class -> value.toString() as V
            Int::class -> convertToInt(value) as V
            Long::class -> convertToLong(value) as V
            Double::class -> convertToDouble(value) as V
            Float::class -> convertToFloat(value) as V
            Boolean::class -> convertToBoolean(value) as V

            // Collection types - simple conversion since generic type info is lost
            List::class -> {
                when (value) {
                    is Collection<*> -> value.toList() as V
                    else -> listOf(value) as V
                }
            }
            Set::class -> {
                when (value) {
                    is Collection<*> -> value.toSet() as V
                    else -> setOf(value) as V
                }
            }
            Map::class -> {
                when (value) {
                    is Map<*, *> -> value as V
                    else ->
                        throw IllegalArgumentException(
                            "Cannot convert ${value::class.simpleName} to ${V::class.simpleName}"
                        )
                }
            }

            // Throw exception if conversion is not possible
            else ->
                throw IllegalArgumentException(
                    "Cannot convert ${value::class.simpleName} to ${V::class.simpleName}"
                )
        }
    }
}

/** Field handler function type */
typealias FieldHandler<T> = (T, Any) -> Unit

/** Patch handler DSL for configuring custom field handlers */
class PatchHandlerDsl<T : Any> {
    val handlers = mutableMapOf<String, FieldHandler<T>>()

    /**
     * Defines a type-safe handler for a specific field
     *
     * @param field The field name to handle
     * @param handler The handler function for the field
     */
    inline fun <reified V> handle(field: String, noinline handler: (entity: T, value: V) -> Unit) {
        handlers[field] = { entity, anyValue ->
            try {
                // Use the type converter
                val convertedValue = TypeConverter.convertTo<V>(anyValue)
                handler(entity, convertedValue)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Error processing field '$field': ${e.message}", e)
            }
        }
    }
}

/**
 * Generic entity patch service for handling PATCH requests
 *
 * Handles entity property updates only, does not persist to database
 */
@Component
class EntityPatcher {
    // Protected system fields that shouldn't be modified via patch
    private val defaultProtectedFields = setOf("id", "createdAt", "updatedAt", "deletedAt")

    /**
     * Applies patch operations to an entity using DSL style configuration
     *
     * @param entity The entity to update
     * @param patchData The patch data (non-null fields)
     * @param additionalProtectedFields Additional fields that should not be modified
     * @param configure Custom handler configuration
     * @return The updated entity (note: not persisted to database)
     */
    fun <T : Any> patch(
        entity: T,
        patchData: Any,
        additionalProtectedFields: Set<String> = emptySet(),
        configure: PatchHandlerDsl<T>.() -> Unit = {},
    ): T {
        val dsl = PatchHandlerDsl<T>().apply(configure)

        // Get all protected fields
        val protectedFields = defaultProtectedFields + additionalProtectedFields

        // Apply patch and return updated entity (caller is responsible for saving)
        return applyPatch(entity, patchData.convertToMap(), dsl.handlers, protectedFields)
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
                    property.visibility?.name == "PRIVATE" &&
                        property.findAnnotation<Patchable>() == null -> false
                    // Allow modification by default
                    else -> true
                }
            }

        // Process each non-null field
        patchMap.forEach { (fieldName, value) ->
            if (value != null) {
                // Use custom handler if available
                if (handlers.containsKey(fieldName)) {
                    handlers[fieldName]?.invoke(entity, value)
                } else {
                    // Find matching property
                    val property = properties.find { it.name == fieldName }
                    if (property != null) {
                        property.isAccessible = true

                        // Convert value type and set
                        val convertedValue = TypeConverter.convert(value, property.returnType)
                        property.set(entity, convertedValue)
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
