package cn.edu.ruc.cheese.backend.common

import org.springframework.http.HttpStatus

class InternalServerError :
        BaseError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "InternalServerError",
                "The server encountered an unexpected error. Please try again later.")
