package org.rucca.cheese.common.query.internal.pagination

import org.rucca.cheese.common.pagination.model.Cursor as LegacyCursor
import org.rucca.cheese.common.pagination.model.CursorValue as LegacyCursorValue
import org.rucca.cheese.common.pagination.model.SimpleCursor as LegacySimpleCursor
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor as LegacyTypedCompositeCursor
import org.rucca.cheese.common.pagination.spec.CursorProjection as LegacyCursorProjection
import org.rucca.cheese.common.pagination.spec.CursorProjectionSupport as LegacyCursorProjectionSupport
import org.rucca.cheese.common.pagination.spec.CursorSpecification as LegacyCursorSpecification

/**
 * Type aliases that allow the new query module to depend on the legacy pagination types without
 * importing the legacy package in every file. Once the underlying implementations are migrated, the
 * aliases can be redirected or dropped altogether.
 */
typealias Cursor<T> = LegacyCursor<T>

typealias CursorValue = LegacyCursorValue

typealias SimpleCursor<T, V> = LegacySimpleCursor<T, V>

typealias TypedCompositeCursor<T> = LegacyTypedCompositeCursor<T>

typealias CursorProjection<T, C> = LegacyCursorProjection<T, C>

typealias CursorProjectionSupport<T, C> = LegacyCursorProjectionSupport<T, C>

typealias CursorSpecification<T, C> = LegacyCursorSpecification<T, C>

val CursorValue.doubleValue: Double?
    get() =
        when (val unwrapped = this.unwrap()) {
            is Number -> unwrapped.toDouble()
            is String -> unwrapped.toDoubleOrNull()
            else -> null
        }
