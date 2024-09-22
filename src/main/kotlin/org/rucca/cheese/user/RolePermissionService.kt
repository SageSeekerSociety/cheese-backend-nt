package org.rucca.cheese.user

import org.rucca.cheese.auth.Authorization
import org.rucca.cheese.auth.AuthorizedResource
import org.rucca.cheese.auth.Permission
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class RolePermissionService {
    fun getAuthorizationForUserWithRole(
            userId: IdType,
            role: String,
    ): Authorization {
        return when (role) {
            "standard-user" -> getAuthorizationForStandardUser(userId)
            else -> throw IllegalArgumentException("Role '$role' is not supported")
        }
    }

    fun getAuthorizationForStandardUser(userId: IdType): Authorization {
        return Authorization(
                userId = userId,
                permissions =
                        listOf(
                                Permission(
                                        authorizedActions =
                                                listOf("ship-ownership", "add-admin", "modify-admin", "remove-admin"),
                                        authorizedResource =
                                                AuthorizedResource(
                                                        types = listOf("space"),
                                                        ownedByUser = userId,
                                                ),
                                ),
                                Permission(
                                        authorizedActions = listOf("modify", "delete"),
                                        authorizedResource =
                                                AuthorizedResource(
                                                        types = listOf("space"),
                                                ),
                                        customLogic = "is-space-admin",
                                ),
                                Permission(
                                        authorizedActions = listOf("query", "create", "enumerate"),
                                        authorizedResource =
                                                AuthorizedResource(
                                                        types = listOf("space"),
                                                ),
                                ),
                                Permission(
                                        authorizedActions =
                                                listOf(
                                                        "ship-ownership",
                                                        "add-admin",
                                                        "remove-admin",
                                                        "modify-member",
                                                        "delete"),
                                        authorizedResource =
                                                AuthorizedResource(
                                                        types = listOf("team"),
                                                        ownedByUser = userId,
                                                ),
                                ),
                                Permission(
                                        authorizedActions =
                                                listOf("add-normal-member", "remove-normal-member", "modify"),
                                        authorizedResource =
                                                AuthorizedResource(
                                                        types = listOf("team"),
                                                ),
                                        customLogic = "is-team-admin",
                                ),
                                Permission(
                                        authorizedActions =
                                                listOf("query", "create", "enumerate-my-teams", "enumerate-members"),
                                        authorizedResource =
                                                AuthorizedResource(
                                                        types = listOf("team"),
                                                ),
                                ),
                        ))
    }
}
