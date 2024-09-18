package org.rucca.cheese.space.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class AlreadyBeSpaceAdminError(spaceId: IdType, userId: IdType) :
        BaseError(
                HttpStatus.CONFLICT,
                "$userId is already an admin of space $spaceId",
                mapOf("spaceId" to spaceId, "userId" to userId))
