/*
 *  Description: Time zone conversion utilities for timestamp operations.
 *
 *  This file provides helper functions for converting between epoch milliseconds (as returned by
 *  JavaScript's Date.getTime()) and Java date-time objects. JavaScript's getTime() returns
 *  milliseconds since the Unix epoch (1970-01-01T00:00:00Z), which is a fixed point in time
 *  regardless of time zones.
 *
 *  IMPORTANT: When handling timestamps:
 *  1. LocalDateTime lacks time zone information, making it unsuitable for timestamp conversions
 *     as it cannot guarantee consistent representation if the system time zone changes.
 *  2. OffsetDateTime is preferred as it preserves time zone offset information, ensuring that
 *     the same instant in time is represented consistently regardless of system time zone changes.
 *  3. These utilities use the system default time zone (ZoneId.systemDefault()) for conversions,
 *     but by using OffsetDateTime, the actual time point remains consistent even if that default changes.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.common.helper

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

fun OffsetDateTime.toEpochMilli(): Long = this.toInstant().toEpochMilli()

@Deprecated(
    "LocalDateTime does not store time zone information, which may cause inconsistent results " +
        "if the system default time zone changes. Use OffsetDateTime.toEpochMilli() instead for " +
        "reliable timestamp operations.",
    ReplaceWith(
        "this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()",
        "java.time.ZoneId",
    ),
)
fun LocalDateTime.toEpochMilli(): Long =
    this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

@Deprecated(
    "Converting to LocalDateTime may lead to inconsistent results across different time zones " +
        "or if the system default time zone changes. Use toOffsetDateTime() instead which explicitly " +
        "preserves time zone information.",
    ReplaceWith("this.toOffsetDateTime()", "org.rucca.cheese.common.helper.toOffsetDateTime"),
)
fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
