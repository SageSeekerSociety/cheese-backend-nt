package org.rucca.cheese.common

import org.springframework.http.HttpStatus

class InternalServerError :
        org.rucca.cheese.common.BaseError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "InternalServerError",
                "The server encountered an unexpected error. Please try again later.")
