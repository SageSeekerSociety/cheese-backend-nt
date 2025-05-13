package org.rucca.cheese.auth.spring

import org.rucca.cheese.auth.core.Role
import org.rucca.cheese.common.persistent.IdType
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class UserPrincipalAuthenticationToken(
    val userId: IdType,
    val systemRoles: Set<Role>,
    authenticated: Boolean = true,
) : AbstractAuthenticationToken(mapRolesToAuthorities(systemRoles)) {

    init {
        super.setAuthenticated(authenticated)
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): Any = userId

    companion object {
        private fun mapRolesToAuthorities(roles: Set<Role>): Collection<GrantedAuthority> {
            return roles.map { SimpleGrantedAuthority("ROLE_${it.roleId.uppercase()}") }.toSet()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserPrincipalAuthenticationToken) return false
        if (!super.equals(other)) return false

        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + userId.hashCode()
        return result
    }
}
