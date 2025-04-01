package org.rucca.cheese.common.pagination.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/** Utility functions for reflection operations, specifically for accessing nested properties. */
object ReflectionUtils {

    private val propertyCache = mutableMapOf<Pair<KClass<*>, String>, KProperty1<Any, *>?>()

    /**
     * Gets the value of a potentially nested property using a path string (e.g.,
     * "user.address.city"). Uses Kotlin reflection with caching for efficiency. Handles null values
     * gracefully during traversal.
     *
     * @param obj The root object from which to extract the value.
     * @param path The property path string.
     * @return The extracted property value, or null if the path is invalid, cannot be accessed, or
     *   any intermediate object in the path is null.
     */
    fun getPropertyValue(obj: Any?, path: String): Any? {
        if (obj == null) return null
        val parts = path.split('.')
        var current: Any? = obj

        try {
            for (part in parts) {
                if (current == null) return null // Stop if intermediate object is null

                val currentClass = current!!::class
                val cacheKey = currentClass to part

                // Cache lookup for property reflection object
                val property =
                    propertyCache.computeIfAbsent(cacheKey) {
                        // Find property using Kotlin reflection (more idiomatic)
                        // Cast needed because KProperty1 is covariant
                        currentClass.memberProperties.find { it.name == part }
                            as? KProperty1<Any, *>
                    }

                if (property == null) {
                    // Property not found on the current object's class
                    // Log warning or handle as needed
                    // logger.warn("Property '$part' not found on class ${currentClass.simpleName}
                    // for path '$path'")
                    return null
                }

                // Get the value of the property for the current object
                current = property.get(current!!)
            }
        } catch (e: Exception) {
            // Catch potential reflection exceptions (e.g., visibility issues, although
            // memberProperties usually handles public ones)
            // logger.error("Error accessing property path '$path' on object $obj: ${e.message}", e)
            return null // Return null on error
        }

        return current
    }
}
