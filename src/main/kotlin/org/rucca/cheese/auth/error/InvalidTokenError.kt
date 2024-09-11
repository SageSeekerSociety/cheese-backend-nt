package org.rucca.cheese.auth.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class InvalidTokenError : BaseError(HttpStatus.UNAUTHORIZED, "InvalidTokenError", "Invalid token")
