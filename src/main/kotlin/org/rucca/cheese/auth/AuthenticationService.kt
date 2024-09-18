package org.rucca.cheese.auth

import org.rucca.cheese.auth.error.AuthenticationRequiredError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class AuthenticationService(
        private val authorizationService: AuthorizationService,
) {
    fun getCurrentUserId(): IdType {
        val token: String =
                (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
                        .request
                        .getHeader("Authorization") ?: throw AuthenticationRequiredError()
        val authorization = authorizationService.verify(token)
        return authorization.userId
    }
}
