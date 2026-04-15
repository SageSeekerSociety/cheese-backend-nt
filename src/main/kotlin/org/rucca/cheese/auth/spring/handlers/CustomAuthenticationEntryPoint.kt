package org.rucca.cheese.auth.spring.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.rucca.cheese.auth.error.*
import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.error.InternalServerError
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(private val objectMapper: ObjectMapper) :
    AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint::class.java)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException, // The exception that caused the failure
    ) {
        logger.debug(
            "Authentication failed: {}. URI: {}",
            authException.message,
            request.requestURI,
        )

        val apiError: BaseError =
            when (authException) {
                is CredentialsExpiredException ->
                    TokenExpiredError(authException.message ?: "Token has expired.")
                is BadCredentialsException ->
                    InvalidTokenError(authException.message ?: "Invalid token or credentials.")
                is InternalAuthenticationServiceException ->
                    InternalServerError(authException.message ?: "Internal authentication error.")
                else ->
                    AuthenticationRequiredError(authException.message ?: "Authentication failed.")
            }

        response.status = apiError.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        try {
            objectMapper.writeValue(response.outputStream, apiError)
        } catch (e: Exception) {
            logger.error("Error writing custom error response for AuthenticationException", e)
            response.sendError(
                apiError.status.value(),
                "Error processing authentication failure: ${apiError.message}",
            )
        }
    }
}
