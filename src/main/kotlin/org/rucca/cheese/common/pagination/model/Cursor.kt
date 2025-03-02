package org.rucca.cheese.common.pagination.model

import kotlin.reflect.KProperty1
import org.rucca.cheese.common.pagination.encoding.CursorConfig

/**
 * Base interface for cursor-based pagination.
 *
 * A cursor represents a position in a result set and is used to efficiently navigate through
 * paginated data.
 */
sealed interface Cursor<T> {
    /** Encode cursor to string representation for API responses. */
    fun encode(): String

    companion object {
        /**
         * Decode string representation into appropriate cursor type.
         *
         * @param encoded The encoded cursor string
         * @return A decoded cursor instance
         */
        fun <T> decode(encoded: String): Cursor<T> {
            return try {
                // Try to decode as composite cursor
                val values = CursorConfig.getEncoder().decode(encoded)
                TypedCompositeCursor(values)
            } catch (e: Exception) {
                // Fall back to simple cursor for backward compatibility
                try {
                    // Try to parse as numeric
                    val numericValue = encoded.toLongOrNull() ?: encoded.toDoubleOrNull()
                    if (numericValue != null) {
                        SimpleCursor(numericValue)
                    } else {
                        // Otherwise treat as string
                        SimpleCursor(encoded)
                    }
                } catch (e: Exception) {
                    // Last resort, treat as string
                    SimpleCursor(encoded)
                }
            }
        }
    }
}

/**
 * Simple cursor implementation for single-value pagination.
 *
 * @param T The entity type
 * @param V The cursor value type
 * @property value The cursor value
 */
data class SimpleCursor<T, V : Any>(val value: V) : Cursor<T> {
    override fun encode(): String = value.toString()
}

/**
 * Type-safe composite cursor for complex pagination scenarios.
 *
 * @param T The entity type
 * @property values Map of property names to typed cursor values
 */
data class TypedCompositeCursor<T>(val values: Map<String, CursorValue>) : Cursor<T> {
    override fun encode(): String {
        return CursorConfig.getEncoder().encode(values)
    }

    companion object {
        /**
         * Create a composite cursor from key-value pairs.
         *
         * @param pairs Property name and value pairs
         * @return A new composite cursor
         */
        fun <T> of(vararg pairs: Pair<String, Any?>): TypedCompositeCursor<T> {
            val cursorValues = pairs.associate { (key, value) -> key to CursorValue.of(value) }
            return TypedCompositeCursor(cursorValues)
        }

        /**
         * Create a composite cursor from entity properties.
         *
         * @param entity Source entity
         * @param properties Properties to extract for the cursor
         * @return A new composite cursor containing the extracted values
         */
        fun <T> from(entity: T, vararg properties: KProperty1<T, *>): TypedCompositeCursor<T> {
            val values =
                properties.associate { property ->
                    val value = property.get(entity)
                    property.name to CursorValue.of(value)
                }
            return TypedCompositeCursor(values)
        }

        /**
         * Decode an encoded cursor string.
         *
         * @param encoded The encoded cursor string
         * @return A new composite cursor
         */
        fun <T> decode(encoded: String): TypedCompositeCursor<T> {
            val values = CursorConfig.getEncoder().decode(encoded)
            return TypedCompositeCursor(values)
        }
    }
}
