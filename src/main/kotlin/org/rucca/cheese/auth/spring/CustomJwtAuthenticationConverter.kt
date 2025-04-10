package org.rucca.cheese.auth.spring

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.rucca.cheese.auth.TokenPayload
import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

// Define as @Bean in SecurityConfig or make it a @Component
@Component // Option 1: Make it a component
class CustomJwtAuthenticationConverter(
    private val userSecurityService: UserSecurityService,
    private val objectMapper: ObjectMapper, // Inject if parsing JSON payload claim
) : Converter<Jwt, AbstractAuthenticationToken> {

    private val logger = LoggerFactory.getLogger(CustomJwtAuthenticationConverter::class.java)

    override fun convert(
        jwt: Jwt
    ): AbstractAuthenticationToken { // Input: Verified Jwt object from Decoder
        try {
            // --- Strategy 1: Extract userId from standard 'sub' claim ---
            val userIdFromSub: IdType? =
                jwt.subject?.let {
                    try {
                        it.toLong()
                    } catch (e: NumberFormatException) {
                        logger.warn("Could not parse user ID from 'sub' claim: {}", it)
                        null // Indicate failure
                    }
                }

            // --- Strategy 2: Extract userId from custom 'payload' claim ---
            // Adjust claim name and parsing based on your *actual* token structure
            val userIdFromPayload: IdType? =
                jwt.getClaim<Any>("payload")?.let { payloadClaim ->
                    try {
                        when (payloadClaim) {
                            is String -> { // If payload is a JSON string
                                val tokenPayload: TokenPayload =
                                    objectMapper.readValue(payloadClaim) // Parse JSON string
                                tokenPayload.authorization.userId
                            }
                            is Map<
                                *,
                                *,
                            > -> { // If payload is already parsed into a Map by the decoder
                                // Assuming structure is Map -> "authorization" -> Map -> "userId"
                                val authMap = payloadClaim["authorization"] as? Map<*, *>
                                val userIdAny = authMap?.get("userId")
                                when (userIdAny) {
                                    is Number -> userIdAny.toLong()
                                    is String -> userIdAny.toLongOrNull()
                                    else -> null
                                }
                            }
                            // Add other potential types for the payload claim if necessary
                            else -> {
                                logger.warn(
                                    "Unexpected type for 'payload' claim: {}",
                                    payloadClaim.javaClass.name,
                                )
                                null
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(
                            "Failed to extract userId from 'payload' claim. Claim content: {}",
                            payloadClaim,
                            e,
                        )
                        null // Indicate failure
                    }
                }

            // --- Determine the final userId ---
            // Prioritize payload claim? Or standard sub claim? Choose one or combine.
            val userId =
                userIdFromPayload
                    ?: userIdFromSub
                    ?: throw BadJwtException(
                        "Could not determine user ID from token claims ('sub' or 'payload')."
                    )

            // --- Load System Roles ---
            val systemRoles: Set<Role> =
                userSecurityService.getUserRoles(userId) // Use the determined userId

            return UserPrincipalAuthenticationToken(userId = userId, systemRoles = systemRoles)
        } catch (e: BadJwtException) {
            throw e // Re-throw exceptions indicating bad token structure/claims
        } catch (e: Exception) {
            logger.error(
                "Unexpected error during JWT to Authentication conversion. JWT Claims: {}",
                jwt.claims,
                e,
            )
            // Wrap unexpected errors
            throw BadJwtException(
                "Failed to convert JWT to authentication token due to internal error.",
                e,
            )
        }
    }
}
