package org.rucca.cheese.team.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class NotTeamMemberYetError(teamId: IdType, userId: IdType) :
    BaseError(
        HttpStatus.NOT_FOUND,
        "User $userId is not a team member of team $teamId yet",
        mapOf("teamId" to teamId, "userId" to userId),
    )
