package org.rucca.cheese.auth.spring

import jakarta.servlet.http.HttpServletRequest
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.common.persistent.IdType
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Argument resolver for @AuthUser annotation. Resolves the current authenticated user from request
 * attributes.
 */
@Component
class AuthUserArgumentResolver(private val userSecurityService: UserSecurityService) :
    HandlerMethodArgumentResolver {

    /** Determines if this resolver supports the parameter. */
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUser::class.java) &&
            parameter.parameterType.isAssignableFrom(AuthUserInfo::class.java)
    }

    /** Resolves the parameter value from the request. */
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw IllegalStateException("Request is not an HttpServletRequest")

        // Get user ID from request attribute (set by JWT interceptor)
        val userId =
            request.getAttribute("userId") as? IdType
                ?: throw IllegalStateException("User ID not found in request")

        return AuthUserInfo(userId, userSecurityService.getUserRoles(userId))
    }
}
