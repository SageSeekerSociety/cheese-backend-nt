package org.rucca.cheese.auth.spring

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

/** Exception thrown when a user does not have permission for an action. */
class AccessDeniedError(message: String) : BaseError(HttpStatus.FORBIDDEN, message)
