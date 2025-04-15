package org.rucca.cheese.auth.spring

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.core.JacksonException // Need for payload parsing
import com.fasterxml.jackson.databind.ObjectMapper // Need for payload parsing
import java.time.Clock
import java.time.Duration
import java.time.Instant // Import Instant
import org.rucca.cheese.auth.TokenPayload // Your payload class
import org.rucca.cheese.common.config.ApplicationConfig
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Component

@Component
class CustomAuth0JwtDecoder(
    applicationConfig: ApplicationConfig,
    private val objectMapper: ObjectMapper, // Inject ObjectMapper to parse custom payload
) : JwtDecoder {
    companion object {
        private val ALLOW_CLOCK_SKEW: Duration = Duration.ofSeconds(60)
    }

    private val logger = LoggerFactory.getLogger(CustomAuth0JwtDecoder::class.java)
    private val clock: Clock = Clock.systemUTC()
    private val verifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(applicationConfig.jwtSecret))
            .acceptLeeway(ALLOW_CLOCK_SKEW.seconds)
            .build()

    override fun decode(token: String): Jwt {
        try {
            val decodedJWT: DecodedJWT = verifier.verify(token)

            val payloadClaim =
                decodedJWT.getClaim("payload")?.toString()
                    ?: throw JwtException("Missing or invalid 'payload' claim in token.")

            val tokenPayload: TokenPayload =
                try {
                    objectMapper.readValue(payloadClaim, TokenPayload::class.java)
                } catch (e: JacksonException) {
                    logger.error("Failed to parse 'payload' claim JSON: {}", payloadClaim, e)
                    throw JwtException("Failed to parse token payload.", e)
                }

            val issuedAt = Instant.ofEpochMilli(tokenPayload.signedAt)
            val expiresAt = Instant.ofEpochMilli(tokenPayload.validUntil)

            if (expiresAt.isBefore(Instant.now())) {
                throw JwtValidationException(
                    "Token has expired based on custom 'validUntil' field.",
                    listOf(),
                )
            }

            val springHeaders: MutableMap<String, Any> = mutableMapOf()
            decodedJWT.algorithm?.let { springHeaders[JoseHeaderNames.ALG] = it }
            decodedJWT.type?.let { springHeaders[JoseHeaderNames.TYP] = it }
            decodedJWT.contentType?.let { springHeaders[JoseHeaderNames.CTY] = it }
            decodedJWT.keyId?.let { springHeaders[JoseHeaderNames.KID] = it }

            if (springHeaders.isEmpty()) {
                logger.warn(
                    "Extracted JWT headers map is empty. This might be unexpected. Decoded Header: {}",
                    decodedJWT.header,
                )
            }

            val springClaims: MutableMap<String, Any?> =
                decodedJWT.claims
                    .mapValues { entry -> entry.value.asObject() }
                    .toMutableMap()
                    .apply {
                        this[JwtClaimNames.SUB] = tokenPayload.authorization.userId.toString()
                        this[JwtClaimNames.IAT] = issuedAt
                        this[JwtClaimNames.EXP] = expiresAt
                    }

            return Jwt(token, issuedAt, expiresAt, springHeaders, springClaims)
        } catch (ex: JWTVerificationException) {
            logger.debug("JWT verification failed using auth0: {}", ex.message)
            throw ex
        } catch (ex: JwtException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Unexpected error decoding JWT: {}", token, ex)
            throw JwtException("Failed to decode token due to an unexpected error.", ex)
        }
    }

    private fun Claim.asObject(): Any? {
        return when {
            this.isNull -> null
            this.asBoolean() != null -> this.asBoolean()
            this.asInt() != null ->
                this.asInt() // Or asLong() / asDouble() based on expected precision
            this.asLong() != null -> this.asLong()
            this.asDouble() != null -> this.asDouble()
            this.asDate() != null -> this.asInstant() // Use Instant
            this.asString() != null -> this.asString()
            this.asMap() != null -> this.asMap()?.mapValues { it.value } // Simple map conversion
            this.asList(Object::class.java) != null ->
                this.asList(Object::class.java) // Get as list
            else -> null // Or throw exception for unsupported type
        }
    }
}
