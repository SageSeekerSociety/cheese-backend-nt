package org.rucca.cheese.team.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class PendingApplicationExistsError(teamId: IdType, userId: IdType) :
    BaseError(
        HttpStatus.CONFLICT,
        "User $userId already has a pending application for team $teamId",
        mapOf("teamId" to teamId, "userId" to userId),
    )
