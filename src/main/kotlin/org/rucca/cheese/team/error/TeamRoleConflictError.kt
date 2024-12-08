/*
 *  Description: This file defines the TeamRoleConflictError class.
 *               It is thrown when a user is already in a team with a different role.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.team.error

import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.team.TeamMemberRole
import org.springframework.http.HttpStatus

class TeamRoleConflictError(
    teamId: IdType,
    userId: IdType,
    already: TeamMemberRole,
    request: TeamMemberRole,
) :
    BaseError(
        HttpStatus.CONFLICT,
        "User $userId in team $teamId is already $already, cannot be $request",
        mapOf(
            "teamId" to teamId,
            "userId" to userId,
            "already" to already,
            "request" to request,
        ),
    )
