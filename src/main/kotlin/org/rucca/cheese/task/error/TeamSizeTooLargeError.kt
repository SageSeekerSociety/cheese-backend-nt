package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class TeamSizeTooLargeError(teamSize: Int, maxSize: Int) :
    BaseError(
        HttpStatus.BAD_REQUEST,
        "Team size is too large, current size is $teamSize, but maximum allowed size is $maxSize.",
        mapOf("teamSize" to teamSize, "maxSize" to maxSize),
    )
