package cn.edu.ruc.cheese.backend.common

import org.springframework.http.HttpStatus

class BadRequestError(message: String) :
        BaseError(
                HttpStatus.BAD_REQUEST,
                "BadRequestError",
                message,
        )
