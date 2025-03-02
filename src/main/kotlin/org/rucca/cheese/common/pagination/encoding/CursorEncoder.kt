package org.rucca.cheese.common.pagination.encoding

import org.rucca.cheese.common.pagination.model.CursorValue

/**
 * Interface for cursor encoding/decoding operations.
 *
 * Cursor encoders handle serialization and deserialization of cursor values for transmission in API
 * responses and requests.
 */
interface CursorEncoder {
    /**
     * Encode cursor values map to string representation.
     *
     * @param values Map of property names to cursor values
     * @return Encoded string suitable for API responses
     */
    fun encode(values: Map<String, CursorValue>): String

    /**
     * Decode encoded string back to cursor values map.
     *
     * @param encoded The encoded cursor string from an API request
     * @return Map of property names to cursor values
     * @throws IllegalArgumentException if the encoded string is invalid
     */
    fun decode(encoded: String): Map<String, CursorValue>
}
