package org.rucca.cheese.auth.spring.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.rucca.cheese.auth.spring.AccessDeniedError
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper // Inject Jackson ObjectMapper
) : AccessDeniedHandler {

    private val logger = LoggerFactory.getLogger(CustomAccessDeniedHandler::class.java)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException, // The exception
    ) {
        logger.debug(
            "Access denied: {}. URI: {}",
            accessDeniedException.message,
            request.requestURI,
        )

        val apiError = AccessDeniedError(accessDeniedException.message ?: "Access Denied.")

        response.status = apiError.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        try {
            objectMapper.writeValue(response.outputStream, apiError)
        } catch (e: Exception) {
            logger.error("Error writing custom error response for AccessDeniedException", e)
            response.sendError(
                apiError.status.value(),
                "Error processing access denied failure: ${apiError.message}",
            )
        }
    }
}
