package org.rucca.cheese.team.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class UserAlreadyMemberError(teamId: IdType, userId: IdType, message: String? = null) :
    BaseError(
        HttpStatus.CONFLICT,
        message ?: "User $userId is already a member of team $teamId",
        mapOf("teamId" to teamId, "userId" to userId),
    )
