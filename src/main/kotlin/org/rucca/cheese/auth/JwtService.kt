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
import java.time.Duration
import java.time.Instant
import org.rucca.cheese.auth.error.AuthenticationRequiredError
import org.rucca.cheese.auth.error.InvalidTokenError
import org.rucca.cheese.auth.error.TokenExpiredError
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class JwtService(applicationConfig: ApplicationConfig, private val objectMapper: ObjectMapper) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val verifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(applicationConfig.jwtSecret)).build()

    fun getToken(): String {
        return (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
            .request
            .getHeader("Authorization") ?: throw AuthenticationRequiredError()
    }

    /**
     * Gets the currently authenticated user's ID from the SecurityContext. Assumes the principal
     * stored by CustomJwtAuthenticationProvider is the String representation of the userId.
     *
     * @return The user ID of the authenticated user.
     * @throws AuthenticationRequiredError if no user is authenticated.
     * @throws IllegalStateException if the authenticated principal is not the expected user ID
     *   format.
     */
    fun getCurrentUserId(): IdType {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication

        if (
            authentication == null ||
                !authentication.isAuthenticated ||
                authentication.principal == null
        ) {
            logger.debug("No authenticated user found in SecurityContext.")
            throw AuthenticationRequiredError("User is not authenticated.")
        }

        return when (val principal = authentication.principal) {
            // "anonymous" ==
            // "org.springframework.security.authentication.AnonymousAuthenticationToken" -> -1L
            is Long -> principal
            is String ->
                try {
                    principal.toLong()
                } catch (e: NumberFormatException) {
                    logger.error(
                        "Authenticated principal '{}' could not be converted to user ID (Long).",
                        principal,
                        e,
                    )
                    throw IllegalStateException(
                        "Authenticated principal is not a valid user ID format."
                    )
                }
            else -> {
                logger.error(
                    "Authenticated principal is not of expected type String: {}",
                    principal.javaClass.name,
                )
                throw IllegalStateException("Unexpected principal type found in SecurityContext.")
            }
        }
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
        val now = Instant.now()
        val signedAtInstant = Instant.ofEpochMilli(payload.signedAt)

        if (signedAtInstant.isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
            val difference = Duration.between(now, signedAtInstant)
            logger.warn(
                "Token timestamp validation failed: 'signedAt' is too far in the future. " +
                    "signedAt: {}, now: {}, difference: {}, allowed skew: {}. Potential clock issue or attack.",
                signedAtInstant,
                now,
                difference,
                ALLOWED_CLOCK_SKEW,
            )
            throw RuntimeException(
                "Token signed too far in the future (exceeds allowed clock skew)."
            )
        }

        return payload.authorization
    }

    companion object {
        val ALLOWED_CLOCK_SKEW: Duration = Duration.ofSeconds(60)
    }
}
