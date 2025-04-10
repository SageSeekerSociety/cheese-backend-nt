package org.rucca.cheese.team.error

import org.rucca.cheese.common.error.ForbiddenError

/**
 * Exception thrown when an operation on a team (like adding/removing members) is forbidden because
 * the team is locked due to participation in certain tasks.
 */
class TeamLockedError(message: String, val details: Map<String, Any> = emptyMap()) :
    ForbiddenError(message, details)
