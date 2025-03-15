/*
 *  Description: This file implements the AuthorizationService class.
 *               It is responsible for verifying the JWT token and auditing the user's actions.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.auth

import javax.annotation.PostConstruct
import org.rucca.cheese.auth.error.PermissionDeniedError
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
    private val jwtService: JwtService,
) {
    val customAuthLogics = CustomAuthLogics()
    val ownerIds = OwnerIds()
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
        audit(jwtService.verify(token), action, resourceType, resourceId, authInfo)
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
}
