/*
 *  Description: This file implements GlobalErrorHandler class.
 *               It handles all exceptions thrown by controllers.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.common.error

import jakarta.servlet.http.HttpServletRequest
import org.rucca.cheese.auth.annotation.NoAuth
import org.rucca.cheese.auth.error.AuthenticationRequiredError
import org.rucca.cheese.auth.spring.AccessDeniedError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class GlobalErrorHandler {
    private val logger = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

    private fun HttpServletRequest.isSSE(): Boolean =
        getHeader("Accept")?.contains(MediaType.TEXT_EVENT_STREAM_VALUE) == true

    private fun ResponseEntity.BodyBuilder.handleSseOrJson(
        request: HttpServletRequest,
        message: String,
        jsonBody: Any,
    ): ResponseEntity<*> {
        return if (request.isSSE()) {
            contentType(MediaType.TEXT_EVENT_STREAM).body("event: error\ndata: $message\n\n")
        } else {
            contentType(MediaType.APPLICATION_JSON).body(jsonBody)
        }
    }

    @ExceptionHandler(BaseError::class)
    @ResponseBody
    fun handleBaseError(e: BaseError, request: HttpServletRequest): ResponseEntity<*> =
        ResponseEntity.status(e.status).handleSseOrJson(request, e.message, e)

    @ExceptionHandler(AuthenticationRequiredError::class)
    @ResponseBody
    @NoAuth
    fun handleAuthenticationRequiredError(
        e: AuthenticationRequiredError,
        request: HttpServletRequest,
    ): ResponseEntity<*> = ResponseEntity.status(e.status).handleSseOrJson(request, e.message, e)

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseBody
    fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<*> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .handleSseOrJson(request, e.message, BadRequestError(e.message))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseBody
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<*> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .handleSseOrJson(request, e.message, BadRequestError(e.message))

    @ExceptionHandler(HttpMessageConversionException::class)
    @ResponseBody
    fun handleHttpMessageConversionException(
        e: HttpMessageConversionException,
        request: HttpServletRequest,
    ): ResponseEntity<*> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .handleSseOrJson(
                request,
                e.message ?: "Invalid request caused http message conversion error",
                BadRequestError(e.message ?: "Invalid request caused http message conversion error"),
            )

    @ExceptionHandler(AccessDeniedError::class)
    @ResponseBody
    fun handleAccessDeniedException(
        e: AccessDeniedError,
        request: HttpServletRequest,
    ): ResponseEntity<*> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).handleSseOrJson(request, e.message, e)

    @ExceptionHandler(NoResourceFoundException::class)
    @ResponseBody
    fun handleNoResourceFoundException(
        e: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<*> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .handleSseOrJson(
                request,
                e.message ?: "Resource not found",
                NotFoundError(e.message ?: "Resource not found"),
            )

    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun handleException(e: Exception, request: HttpServletRequest): ResponseEntity<*> {
        logger.error("Unexpected error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .handleSseOrJson(request, "Internal server error", InternalServerError())
    }
}
