package org.rucca.cheese.task.error

import org.rucca.cheese.common.error.BaseError
import org.springframework.http.HttpStatus

class TeamSizeNotEnoughError(teamSize: Int, minSize: Int) :
    BaseError(
        HttpStatus.FORBIDDEN,
        "Team size is not enough, current size is $teamSize, but minimum required size is $minSize.",
        mapOf("teamSize" to teamSize, "minSize" to minSize),
    )
