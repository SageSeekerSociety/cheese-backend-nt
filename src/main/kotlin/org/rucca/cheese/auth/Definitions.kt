package org.rucca.cheese.auth

import org.rucca.cheese.common.persistent.IdType

typealias AuthorizedAction = String

data class AuthorizedResource(
        val ownedByUser: IdType?,
        val types: List<String>?,
        val resourceIds: List<IdType>?,
        val data: Any?,
)

data class Permission(
        val authorizedActions: List<AuthorizedAction>?,
        val authorizedResource: AuthorizedResource,
        val customLogic: String?,
        val customLogicData: Any?,
)

data class Authorization(
        val userId: IdType,
        val permissions: List<Permission>,
)

data class TokenPayload(
        val authorization: Authorization,
        val signedAt: Long,
        val validUntil: Long,
)
