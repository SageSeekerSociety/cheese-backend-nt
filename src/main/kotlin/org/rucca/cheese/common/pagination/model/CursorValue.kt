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
sealed class CursorValue {
    /** Convert to JSON element for serialization. */
    abstract fun toJsonElement(): JsonElement

    /** String cursor value. */
    @Serializable
    data class StringValue(val value: String) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)
    }

    /** Long integer cursor value. */
    @Serializable
    data class LongValue(val value: Long) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)
    }

    /** Double precision floating point cursor value. */
    @Serializable
    data class DoubleValue(val value: Double) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)
    }

    /** Boolean cursor value. */
    @Serializable
    data class BooleanValue(val value: Boolean) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)
    }

    /** Timestamp cursor value (stored as milliseconds). */
    @Serializable
    data class TimestampValue(val value: Long) : CursorValue() {
        override fun toJsonElement(): JsonElement = JsonPrimitive(value)

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
    }

    companion object {
        /**
         * Create appropriate CursorValue from any value.
         *
         * @param value Source value of any supported type
         * @return Typed CursorValue instance
         */
        fun of(value: Any?): CursorValue {
            return when (value) {
                null -> NullValue
                is String -> StringValue(value)
                is Int -> LongValue(value.toLong())
                is Long -> LongValue(value)
                is Float -> DoubleValue(value.toDouble())
                is Double -> DoubleValue(value)
                is Boolean -> BooleanValue(value)
                is Date -> TimestampValue.fromDate(value)
                is Instant -> TimestampValue.fromInstant(value)
                is LocalDate ->
                    TimestampValue.fromInstant(
                        value.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    )
                is LocalDateTime ->
                    TimestampValue.fromInstant(value.atZone(ZoneId.systemDefault()).toInstant())
                else -> StringValue(value.toString())
            }
        }

        /** Create CursorValue from JSON element. */
        fun fromJsonElement(element: JsonElement): CursorValue {
            return when (element) {
                is JsonPrimitive -> {
                    when {
                        element.isString -> StringValue(element.content)
                        element.content == "null" -> NullValue
                        element.content == "true" || element.content == "false" ->
                            BooleanValue(element.boolean)
                        element.content.contains(".") -> DoubleValue(element.double)
                        else ->
                            try {
                                LongValue(element.long)
                            } catch (e: Exception) {
                                StringValue(element.content)
                            }
                    }
                }
                is JsonNull -> NullValue
                else -> StringValue(element.toString())
            }
        }
    }
}

/** Convert JsonElement to CursorValue. */
fun JsonElement.toCursorValue(): CursorValue {
    return CursorValue.fromJsonElement(this)
}

/** Access string value from any CursorValue type. */
val CursorValue.stringValue: String?
    get() =
        when (this) {
            is CursorValue.StringValue -> value
            is CursorValue.LongValue -> value.toString()
            is CursorValue.DoubleValue -> value.toString()
            is CursorValue.BooleanValue -> value.toString()
            is CursorValue.TimestampValue -> value.toString()
            CursorValue.NullValue -> null
        }

/** Access long value from compatible CursorValue types. */
val CursorValue.longValue: Long?
    get() =
        when (this) {
            is CursorValue.LongValue -> value
            is CursorValue.DoubleValue -> value.toLong()
            is CursorValue.StringValue -> value.toLongOrNull()
            is CursorValue.TimestampValue -> value
            else -> null
        }

/** Access double value from compatible CursorValue types. */
val CursorValue.doubleValue: Double?
    get() =
        when (this) {
            is CursorValue.DoubleValue -> value
            is CursorValue.LongValue -> value.toDouble()
            is CursorValue.StringValue -> value.toDoubleOrNull()
            is CursorValue.TimestampValue -> value.toDouble()
            else -> null
        }

/** Access boolean value from compatible CursorValue types. */
val CursorValue.booleanValue: Boolean?
    get() =
        when (this) {
            is CursorValue.BooleanValue -> value
            is CursorValue.StringValue -> value.lowercase() == "true"
            else -> null
        }

/** Access timestamp value from compatible CursorValue types. */
val CursorValue.timestampValue: Long?
    get() =
        when (this) {
            is CursorValue.TimestampValue -> value
            is CursorValue.LongValue -> value
            is CursorValue.StringValue -> value.toLongOrNull()
            else -> null
        }
