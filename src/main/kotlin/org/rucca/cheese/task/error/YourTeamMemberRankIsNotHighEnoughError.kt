package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.springframework.http.HttpStatus

class YourTeamMemberRankIsNotHighEnoughError(userId: IdType, requiredRank: Int, actualRank: Int) :
    BaseError(
        HttpStatus.FORBIDDEN,
        "your team member (ID: $userId) has rank $actualRank, but the required rank is $requiredRank",
        mapOf("userId" to userId, "requiredRank" to requiredRank, "actualRank" to actualRank),
    )
