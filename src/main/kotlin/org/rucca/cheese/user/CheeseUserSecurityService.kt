package org.rucca.cheese.user

import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.auth.core.SystemRole
import org.rucca.cheese.auth.spring.UserSecurityService
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.UserRole.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class CheeseUserSecurityService(private val userService: UserService) : UserSecurityService {
    private val logger = LoggerFactory.getLogger(CheeseUserSecurityService::class.java)

    @Cacheable("userRoles", key = "#userId")
    override fun getUserRole(userId: IdType): Set<Role> {
        logger.info("Looking for user role {}", userId)

        if (userId < 0) return setOf(SystemRole.GUEST)

        return setOf(userService.getUserRole(userId).toSystemRole())
    }
}

private fun UserRole.toSystemRole(): Role {
    return when (this) {
        ADMIN -> SystemRole.ADMIN
        MODERATOR -> SystemRole.MODERATOR
        USER -> SystemRole.USER
    }
}
