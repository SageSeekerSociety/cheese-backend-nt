package org.rucca.cheese.auth.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class TokenExpiredError : BaseError(HttpStatus.UNAUTHORIZED, "TokenExpiredError", "Token has expired")
