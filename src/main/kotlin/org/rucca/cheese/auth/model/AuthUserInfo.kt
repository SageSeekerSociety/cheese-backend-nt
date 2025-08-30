package org.rucca.cheese.auth.model

import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.common.persistent.IdType

/** Container for authenticated user information. This class holds user data extracted from JWT. */
data class AuthUserInfo(val userId: IdType, val userRoles: Set<Role> = emptySet()) {
    val isGuest: Boolean
        get() = userId < 0
}
