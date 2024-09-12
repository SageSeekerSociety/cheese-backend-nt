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
import org.rucca.cheese.auth.error.PermissionDeniedError
import org.rucca.cheese.auth.error.TokenExpiredError
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthorizationService(
        applicationConfig: ApplicationConfig,
        private val objectMapper: ObjectMapper,
) {
    val customAuthLogics = CustomAuthLogics()
    val ownerIds = OwnerIds()
    private val verifier: JWTVerifier = JWT.require(Algorithm.HMAC256(applicationConfig.jwtSecret)).build()
    private val logger = LoggerFactory.getLogger(AuthorizationService::class.java)

    fun audit(
            token: String?,
            action: String,
            resourceType: String,
            resourceId: IdType?,
    ) {
        audit(verify(token), action, resourceType, resourceId)
    }

    fun audit(
            authorization: Authorization,
            action: String,
            resourceType: String,
            resourceId: IdType?,
    ) {
        val userId = authorization.userId
        val ownerIdGetter = if (resourceId != null) ownerIds.getOwnerIdGetter(resourceType, resourceId) else null
        for (permission in authorization.permissions) {
            if (!(permission.authorizedActions == null || permission.authorizedActions.contains(action))) continue
            if (!(permission.authorizedResource.ownedByUser == null ||
                    permission.authorizedResource.ownedByUser == ownerIdGetter?.invoke()))
                    continue
            if (!(permission.authorizedResource.types == null ||
                    permission.authorizedResource.types.contains(resourceType)))
                    continue
            if (!(permission.authorizedResource.resourceIds == null ||
                    permission.authorizedResource.resourceIds.contains(resourceId)))
                    continue
            if (permission.customLogic != null) {
                val result =
                        customAuthLogics.invoke(
                                permission.customLogic,
                                userId,
                                action,
                                resourceType,
                                resourceId,
                                ownerIdGetter,
                                permission.customLogicData)
                if (!result) continue
            }
            logger.info(
                    "Operation permitted: '$action' on resource (resourceType: '$resourceType', resourceId: $resourceId)." +
                            " Authorization: $authorization")
            return
        }
        logger.warn(
                "Operation denied: '$action' on resource (resourceType: '$resourceType', resourceId: $resourceId)." +
                        " Authorization: $authorization")
        throw PermissionDeniedError(action, resourceType, resourceId)
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
                            TokenPayload::class.java)
        } catch (e: TokenExpiredException) {
            throw TokenExpiredError()
        } catch (e: JWTVerificationException) {
            throw InvalidTokenError()
        } catch (e: JacksonException) {
            throw RuntimeException(
                    "The token is valid, but the payload of the token is not a TokenPayload object." +
                            " This is ether a bug or a malicious attack.",
                    e)
        }

        if (payload.validUntil < System.currentTimeMillis()) throw TokenExpiredError()
        if (payload.signedAt > System.currentTimeMillis())
                throw RuntimeException(
                        "The token is valid, but it was signed in the future." +
                                " This is a timezone bug or a malicious attack.")

        return payload.authorization
    }
}
