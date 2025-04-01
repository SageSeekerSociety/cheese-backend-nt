package org.rucca.cheese.common.pagination.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Type-safe representation of cursor values.
 *
 * This sealed class hierarchy provides type safety for cursor values and supports
 * serialization/deserialization.
 */
@Serializable
sealed class CursorValue :
    java.io.Serializable { // Ensure it's Serializable for potential caching etc.
    /** Convert to JSON element for serialization. */
    abstract fun toJsonElement(): JsonElement

    /**
     * Unwraps the stored value to its original type (String, Long, Double, Boolean, Long for
     * timestamp) or null. This is useful for comparing values or using them in JPA Criteria
     * Predicates.
     *
     * @return The underlying value as Any?, or null if it's NullValue.
     */
    abstract fun unwrap(): Any?

    /** String cursor value. */
    @Serializable
    data class StringValue(val value: String) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)

        override fun unwrap(): Any = value // Returns String
    }

    /** Long integer cursor value. */
    @Serializable
    data class LongValue(val value: Long) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)

        override fun unwrap(): Any = value // Returns Long
    }

    /** Double precision floating point cursor value. */
    @Serializable
    data class DoubleValue(val value: Double) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)

        override fun unwrap(): Any = value // Returns Double
    }

    /** Boolean cursor value. */
    @Serializable
    data class BooleanValue(val value: Boolean) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)

        override fun unwrap(): Any = value // Returns Boolean
    }

    /**
     * Timestamp cursor value (stored as milliseconds since epoch). Note: unwrap() returns the Long
     * value. Use helper properties like `instantValue` if needed.
     */
    @Serializable
    data class TimestampValue(val value: Long) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)

        override fun unwrap(): Any = value // Returns Long (milliseconds)

        companion object {
            /** Create from java.util.Date. */
            fun fromDate(date: Date): TimestampValue = TimestampValue(date.time)

            /** Create from java.time.Instant. */
            fun fromInstant(instant: Instant): TimestampValue =
                TimestampValue(instant.toEpochMilli())
        }
    }

    /** Null cursor value. */
    @Serializable
    data object NullValue : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonNull

        override fun unwrap(): Any? = null // Returns null
    }

    companion object {
        /**
         * Create appropriate CursorValue from any value. Handles common types including primitives,
         * Dates, and java.time objects. Unrecognized types are converted to String.
         *
         * @param value Source value of any supported type
         * @return Typed CursorValue instance
         */
        fun of(value: Any?): CursorValue {
            return when (value) {
                null -> NullValue
                is String -> StringValue(value)
                is Int -> LongValue(value.toLong()) // Standardize Int to Long
                is Long -> LongValue(value)
                is Float -> DoubleValue(value.toDouble()) // Standardize Float to Double
                is Double -> DoubleValue(value)
                is Boolean -> BooleanValue(value)
                // Time-based types standardized to timestamp (Long)
                is Date -> TimestampValue.fromDate(value)
                is Instant -> TimestampValue.fromInstant(value)
                is LocalDate ->
                    TimestampValue.fromInstant(
                        value.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    )
                is LocalDateTime ->
                    TimestampValue.fromInstant(value.atZone(ZoneId.systemDefault()).toInstant())
                // Fallback for unrecognized types
                else -> {
                    // Consider logging a warning for unexpected types
                    // logger.warn("Unsupported type encountered in CursorValue.of:
                    // ${value::class.simpleName}. Converting to String.")
                    StringValue(value.toString())
                }
            }
        }

        /**
         * Create CursorValue from JSON element. Attempts to infer type. Note: TimestampValue is
         * typically represented as a Long in JSON. This function might misinterpret a Long JSON
         * number intended as a timestamp if not used carefully in conjunction with schema/context.
         */
        fun fromJsonElement(element: JsonElement): CursorValue {
            return when (element) {
                is JsonNull -> NullValue
                is JsonPrimitive -> {
                    when {
                        element.isString -> StringValue(element.content)
                        // Explicit null string check might be needed depending on serialization
                        // source
                        // element.content.equals("null", ignoreCase = true) -> NullValue
                        element.booleanOrNull != null -> BooleanValue(element.boolean)
                        element.longOrNull != null -> LongValue(element.long) // Check Long first
                        element.doubleOrNull != null -> DoubleValue(element.double) // Then Double
                        else ->
                            StringValue(
                                element.content
                            ) // Fallback to String if no other type matches
                    }
                }
                // Handle JsonObject or JsonArray as String or throw error?
                else -> StringValue(element.toString()) // Fallback for complex types
            }
        }
    }
}

// --- Extension Properties (Existing - Keep them as they provide convenient typed access) ---

/** Convert JsonElement to CursorValue. */
fun JsonElement.toCursorValue(): CursorValue {
    return CursorValue.fromJsonElement(this)
}

/** Access string value from any CursorValue type (lossy for non-string types). */
val CursorValue.stringValue: String?
    get() = this.unwrap()?.toString() // Simplified using unwrap

/** Access long value from compatible CursorValue types (potential precision loss or null). */
val CursorValue.longValue: Long?
    get() =
        when (val unwrapped = this.unwrap()) {
            is Long -> unwrapped
            is Number -> unwrapped.toLong() // Handles Double, Float, Int etc.
            is String -> unwrapped.toLongOrNull()
            else -> null
        }

/** Access double value from compatible CursorValue types (potential precision loss or null). */
val CursorValue.doubleValue: Double?
    get() =
        when (val unwrapped = this.unwrap()) {
            is Double -> unwrapped
            is Number -> unwrapped.toDouble() // Handles Long, Float, Int etc.
            is String -> unwrapped.toDoubleOrNull()
            else -> null
        }

/** Access boolean value from compatible CursorValue types. */
val CursorValue.booleanValue: Boolean?
    get() =
        when (val unwrapped = this.unwrap()) {
            is Boolean -> unwrapped
            is String -> unwrapped.equals("true", ignoreCase = true) // Common string representation
            is Number -> unwrapped.toInt() != 0 // Common numeric representation
            else -> null
        }

/** Access timestamp value (as Long milliseconds) from compatible CursorValue types. */
val CursorValue.timestampValue: Long?
    get() =
        when (this) { // Keep original logic as TimestampValue always unwraps to Long
            is CursorValue.TimestampValue -> value
            is CursorValue.LongValue -> value // Allow direct use of LongValue as timestamp
            is CursorValue.StringValue -> value.toLongOrNull()
            else -> null
        }

/** Convenience property to get Instant from TimestampValue. */
val CursorValue.instantValue: Instant?
    get() = if (this is CursorValue.TimestampValue) Instant.ofEpochMilli(this.value) else null

/** Convenience property to get LocalDateTime from TimestampValue (uses system default zone). */
val CursorValue.localDateTimeValue: LocalDateTime?
    get() = this.instantValue?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
