package org.rucca.cheese.user

import jakarta.annotation.PostConstruct
import org.rucca.cheese.auth.context.ContextKey
import org.rucca.cheese.auth.context.DomainContextKeys
import org.rucca.cheese.auth.context.PermissionContextProvider
import org.rucca.cheese.auth.context.buildResourceContext
import org.rucca.cheese.auth.core.*
import org.rucca.cheese.auth.domain.DomainPermissionService
import org.rucca.cheese.auth.domain.DomainRoleProvider
import org.rucca.cheese.auth.dsl.applyHierarchy
import org.rucca.cheese.auth.dsl.definePermissions
import org.rucca.cheese.auth.dsl.defineRoleHierarchy
import org.rucca.cheese.auth.hierarchy.GraphRoleHierarchy
import org.rucca.cheese.auth.registry.RegistrationService
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
object UserDomain : Domain {
    override val name: String = "user"
}

enum class UserAction(override val actionId: String) : Action {
    VIEW("view"),
    UPDATE("update");

    override val domain: Domain = UserDomain
}

enum class UserResource(override val typeName: String) : ResourceType {
    IDENTITY("identity"),
    ACCESS_LOG("access_log");

    override val domain: Domain = UserDomain

    companion object {
        fun of(typeName: String) =
            entries.find { it.typeName == typeName } ?: error("Invalid resource type")
    }
}

enum class UserDomainRole(override val roleId: String) : Role {
    SELF("self"),
    ADMIN("admin");

    override val domain: Domain? = UserDomain
}

@Component
class UserRoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(UserDomainRole.ADMIN)
            role(UserDomainRole.SELF)
        }

        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}

object UserIdentityContextKeys {
    val TARGET_USER_ID = ContextKey.of<IdType>("targetUserId")
}

@Component
class UserIdentityContextProvider : PermissionContextProvider {
    override val domain: Domain = UserDomain

    override fun getContext(resourceName: String, resourceId: IdType?) =
        buildResourceContext(domain, UserResource.of(resourceName), resourceId) {
            if (resourceId != null) {
                UserIdentityContextKeys.TARGET_USER_ID(resourceId)
            }
        }
}

@Component
class UserRoleProvider : DomainRoleProvider {
    private val logger = LoggerFactory.getLogger(UserRoleProvider::class.java)

    override val domain: Domain = UserDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val roles = mutableSetOf<Role>()

        when (DomainContextKeys.RESOURCE_TYPE.get(context)?.let { UserResource.of(it) }) {
            UserResource.IDENTITY,
            UserResource.ACCESS_LOG -> {
                val targetUserId =
                    UserIdentityContextKeys.TARGET_USER_ID.get(context) ?: return roles

                if (userId == targetUserId) {
                    roles.add(UserDomainRole.SELF)
                }
            }
            null -> {}
        }

        return roles.toSet()
    }
}

@Component
class UserIdentityPermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) : DomainPermissionService {
    private val logger = LoggerFactory.getLogger(UserIdentityPermissionConfig::class.java)

    override val domain: Domain = UserDomain

    @PostConstruct
    override fun configurePermissions() {
        registrationService.registerActions(*UserAction.entries.toTypedArray())
        registrationService.registerResources(*UserResource.entries.toTypedArray())

        val config = definePermissions {
            role(UserDomainRole.SELF) {
                can(UserAction.VIEW, UserAction.UPDATE)
                    .on(UserResource.IDENTITY, UserResource.ACCESS_LOG)
                    .all()
            }
        }

        permissionService.applyConfiguration(config)
    }
}
