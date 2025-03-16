package org.rucca.cheese.auth.core

/**
 * Represents a role within the authorization system. Roles are assigned to users and determine what
 * permissions they have.
 */
interface Role {
    /**
     * The domain that the role belongs to.
     *
     * `null` means the role is not domain-specific (or system role).
     */
    val domain: Domain?

    /** The unique identifier of the role. */
    val roleId: String
}

fun Role.toDomainRoleId() = domain?.let { "${it.name}:$roleId" } ?: roleId

/** Standard system roles that should be available in all applications. */
enum class SystemRole(override val roleId: String) : Role {
    SUPER_ADMIN("super_admin"),
    ADMIN("admin"),
    MODERATOR("moderator"),
    USER("user"),
    GUEST("guest");

    override val domain: Domain? = null
}
