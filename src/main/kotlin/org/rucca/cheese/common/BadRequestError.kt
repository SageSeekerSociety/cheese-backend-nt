package org.rucca.cheese.common

import org.springframework.http.HttpStatus

class BadRequestError(message: String) :
        org.rucca.cheese.common.BaseError(
                HttpStatus.BAD_REQUEST,
                "BadRequestError",
                message,
        )
