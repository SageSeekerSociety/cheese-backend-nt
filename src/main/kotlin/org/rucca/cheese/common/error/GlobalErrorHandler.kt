package org.rucca.cheese.common.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice
class GlobalErrorHandler {
    private val logger = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

    @ExceptionHandler(BaseError::class)
    @ResponseBody
    fun handleBaseError(e: BaseError): ResponseEntity<BaseError> {
        return ResponseEntity.status(e.status).body(e)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseBody
    fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException
    ): ResponseEntity<BaseError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BadRequestError(e.message))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseBody
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException
    ): ResponseEntity<BaseError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(BadRequestError(e.message ?: "Method argument type mismatch"))
    }

    @ExceptionHandler(HttpMessageConversionException::class)
    @ResponseBody
    fun handleHttpMessageConversionException(
        e: HttpMessageConversionException
    ): ResponseEntity<BaseError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                BadRequestError(e.message ?: "Invalid request caused http message conversion error")
            )
    }

    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun handleException(e: Exception): ResponseEntity<BaseError> {
        logger.error("Unexpected error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(InternalServerError())
    }
}
