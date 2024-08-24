package org.rucca.cheese.auth

import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class AuthorizationService {
    fun audit(
            token: String?,
            action: String,
            resourceType: String,
            resourceId: IdType?,
    ) {}
}
