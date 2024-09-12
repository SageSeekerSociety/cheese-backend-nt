package org.rucca.cheese.common.error

import org.springframework.http.HttpStatus

class BadRequestError(message: String) :
        BaseError(
                HttpStatus.BAD_REQUEST,
                "BadRequestError",
                message,
        )
