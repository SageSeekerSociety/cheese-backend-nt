package org.rucca.cheese.common.error

import org.springframework.http.HttpStatus

class InternalServerError :
        BaseError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The server encountered an unexpected error. Please try again later.")
