package org.rucca.cheese.auth.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class AuthenticationRequiredError :
    BaseError(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource")
