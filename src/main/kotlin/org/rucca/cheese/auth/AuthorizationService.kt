package org.rucca.cheese.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import javax.annotation.PostConstruct
import org.rucca.cheese.auth.error.AuthenticationRequiredError
import org.rucca.cheese.auth.error.InvalidTokenError
import org.rucca.cheese.auth.error.PermissionDeniedError
import org.rucca.cheese.auth.error.TokenExpiredError
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class AuthorizationService(
    private val applicationConfig: ApplicationConfig,
    private val objectMapper: ObjectMapper,
) {
    val customAuthLogics = CustomAuthLogics()
    val ownerIds = OwnerIds()
    private val verifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(applicationConfig.jwtSecret)).build()
    private val logger = LoggerFactory.getLogger(AuthorizationService::class.java)

    @PostConstruct
    fun initialize() {
        this.customAuthLogics.register("owned") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            _: Map<String, Any>,
            resourceOwnerIdGetter: IdGetter?,
            _: Any? ->
            if (resourceOwnerIdGetter == null) false else resourceOwnerIdGetter() == userId
        }
    }

    fun audit(
        action: String,
        resourceType: String,
        resourceId: IdType?,
        authInfo: Map<String, Any> = emptyMap(),
    ) {
        val token: String? =
            (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
                .request
                .getHeader("Authorization")
        audit(token, action, resourceType, resourceId, authInfo)
    }

    fun audit(
        token: String?,
        action: String,
        resourceType: String,
        resourceId: IdType?,
        authInfo: Map<String, Any> = emptyMap(),
    ) {
        audit(verify(token), action, resourceType, resourceId, authInfo)
    }

    fun audit(
        authorization: Authorization,
        action: String,
        resourceType: String,
        resourceId: IdType?,
        authInfo: Map<String, Any> = emptyMap(),
    ) {
        val userId = authorization.userId
        val ownerIdGetter =
            if (resourceId != null) ownerIds.getOwnerIdGetter(resourceType, resourceId) else null
        for (permission in authorization.permissions) {
            if (
                !(permission.authorizedActions == null ||
                    permission.authorizedActions.contains(action))
            )
                continue
            if (permission.authorizedResource.ownedByUser != null)
                logger.warn("ownedByUser is deprecated. Use custom logic 'owned' instead.")
            if (
                !(permission.authorizedResource.ownedByUser == null ||
                    permission.authorizedResource.ownedByUser == ownerIdGetter?.invoke())
            )
                continue
            if (
                !(permission.authorizedResource.types == null ||
                    permission.authorizedResource.types.contains(resourceType))
            )
                continue
            if (
                !(permission.authorizedResource.resourceIds == null ||
                    permission.authorizedResource.resourceIds.contains(resourceId))
            )
                continue
            if (permission.customLogic != null) {
                val result =
                    customAuthLogics.evaluate(
                        permission.customLogic,
                        userId,
                        action,
                        resourceType,
                        resourceId,
                        authInfo,
                        ownerIdGetter,
                        permission.customLogicData,
                    )
                if (!result) continue
            }
            return
        }
        if (applicationConfig.warnAuditFailure)
            logger.warn(
                "Operation denied: '$action' on resource (resourceType: '$resourceType', resourceId: $resourceId, authInfo: $authInfo)." +
                    " UserId: $userId. Authorization: $authorization"
            )
        throw PermissionDeniedError(action, resourceType, resourceId, authInfo)
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
