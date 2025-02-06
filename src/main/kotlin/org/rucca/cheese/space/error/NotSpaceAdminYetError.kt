package org.rucca.cheese.space.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class NotSpaceAdminYetError(spaceId: IdType, userId: IdType) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "User $userId is not a space admin of space $spaceId yet",
        mapOf("spaceId" to spaceId, "userId" to userId),
    )
