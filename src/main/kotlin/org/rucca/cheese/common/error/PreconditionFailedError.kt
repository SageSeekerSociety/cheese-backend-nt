package org.rucca.cheese.common.error

import org.springframework.http.HttpStatus

/**
 * Error indicating that a precondition for the requested operation was not met.
 * Maps to HTTP 412 Precondition Failed.
 */
class PreconditionFailedError(
    message: String,
    data: Map<String, Any>? = null // Optional data for more context
) : BaseError(HttpStatus.PRECONDITION_FAILED, message, data)