package org.rucca.cheese.auth.error

import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class PermissionDeniedError(
        action: AuthorizedAction,
        resourceType: String,
        resourceId: IdType?,
) :
        BaseError(
                HttpStatus.FORBIDDEN,
                "The attempt to perform action '$action' on resource (resourceType: '$resourceType', resourceId: $resourceId) is not permitted by the given token.",
                mapOf(
                        "action" to action,
                        "resourceType" to resourceType,
                        "resourceId" to resourceId,
                ))
