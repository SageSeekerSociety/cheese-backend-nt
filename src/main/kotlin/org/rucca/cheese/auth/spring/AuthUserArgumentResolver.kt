package org.rucca.cheese.auth.spring

import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.model.AuthUserInfo
import org.rucca.cheese.common.persistent.IdType
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
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
class AuthUserArgumentResolver : HandlerMethodArgumentResolver {
    private fun getCurrentUserPrincipalAuthentication(): UserPrincipalAuthenticationToken {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication as? UserPrincipalAuthenticationToken
            ?: throw IllegalStateException(
                "Authentication object in SecurityContextHolder is not UserPrincipalAuthenticationToken or is null. Found: ${authentication?.javaClass?.name}"
            )
    }

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
        val currentAuth = getCurrentUserPrincipalAuthentication()

        val userId: IdType = currentAuth.userId
        val systemRoles: Set<Role> = currentAuth.systemRoles

        return AuthUserInfo(userId, systemRoles)
    }
}
