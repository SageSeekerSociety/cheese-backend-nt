package org.rucca.cheese.common.error

import org.springframework.http.HttpStatus

open class ForbiddenError(message: String = "Forbidden", data: Map<String, Any> = emptyMap()) :
    BaseError(status = HttpStatus.FORBIDDEN, message = message, data = data) {
    constructor(
        action: String,
        resource: String,
    ) : this(
        message = "Action $action on resource $resource is forbidden",
        data = mapOf("action" to action, "resource" to resource),
    )
}
