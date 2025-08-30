/*
 *  Description: This file implements the AuthenticationService class.
 *               It is responsible for providing the current user's authentication information.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import org.rucca.cheese.auth.error.AuthenticationRequiredError
import org.rucca.cheese.auth.error.InvalidTokenError
import org.rucca.cheese.auth.error.TokenExpiredError
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class JwtService(applicationConfig: ApplicationConfig, private val objectMapper: ObjectMapper) {
    private val verifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(applicationConfig.jwtSecret)).build()

    fun getToken(): String {
        return (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
            .request
            .getHeader("Authorization") ?: throw AuthenticationRequiredError()
    }

    fun getCurrentUserId(): IdType {
        val token = getToken()
        val authorization = verify(token)
        return authorization.userId
    }

    fun verify(token: String?): Authorization {
        var tokenWithoutBearer = token
        if (token.isNullOrEmpty()) throw AuthenticationRequiredError()
        if (token.indexOf("Bearer ") == 0) tokenWithoutBearer = token.substring(7)
        else if (token.indexOf("bearer ") == 0) tokenWithoutBearer = token.substring(7)

        val payload: TokenPayload
        try {
            payload =
                objectMapper.readValue(
                    verifier.verify(tokenWithoutBearer).getClaim("payload")?.toString()
                        ?: throw InvalidTokenError(),
                    TokenPayload::class.java,
                )
        } catch (e: TokenExpiredException) {
            throw TokenExpiredError()
        } catch (e: JWTVerificationException) {
            throw InvalidTokenError()
        } catch (e: JacksonException) {
            throw RuntimeException(
                "The token is valid, but the payload of the token is not a TokenPayload object." +
                    " This is ether a bug or a malicious attack.",
                e,
            )
        }

        if (payload.validUntil < System.currentTimeMillis()) throw TokenExpiredError()
        if (payload.signedAt > System.currentTimeMillis())
            throw RuntimeException(
                "The token is valid, but it was signed in the future." +
                    " This is a timezone bug or a malicious attack."
            )

        return payload.authorization
    }
}
