package org.rucca.cheese.auth.spring

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.rucca.cheese.auth.JwtService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthenticationInterceptor(
    private val jwtService: JwtService,
    private val userSecurityService: UserSecurityService,
) : HandlerInterceptor {
    companion object {
        private const val GUEST_USER_ID: Long = -1L
    }

    private val logger = LoggerFactory.getLogger(AuthenticationInterceptor::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")

        if (token.isNullOrBlank()) {
            handleGuestAccess(request)
            return true
        }

        val authorization = jwtService.verify(token)
        val userId = authorization.userId

        request.setAttribute("userId", userId)
        request.setAttribute("userRole", userSecurityService.getUserRoles(userId))
        return true
    }

    private fun handleGuestAccess(request: HttpServletRequest) {
        request.setAttribute("userId", GUEST_USER_ID)
        request.setAttribute("userRole", userSecurityService.getUserRoles(GUEST_USER_ID))
    }
}
