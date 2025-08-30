/*
 *  Description: This file implements the RolePermissionService class.
 *               It provides the permissions for different roles.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      nameisyui
 *
 */

package org.rucca.cheese.user

import org.rucca.cheese.auth.Authorization
import org.rucca.cheese.auth.AuthorizedResource
import org.rucca.cheese.auth.Permission
import org.rucca.cheese.common.persistent.IdType
import org.springframework.stereotype.Service

@Service
class RolePermissionService {
    fun getAuthorizationForUserWithRole(userId: IdType, role: String): Authorization {
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
                    // AI permissions
                    Permission(
                        authorizedActions = listOf("query"),
                        authorizedResource = AuthorizedResource(types = listOf("ai:quota")),
                    ),
                    Permission(
                        authorizedActions = listOf("query", "create", "delete"),
                        authorizedResource = AuthorizedResource(types = listOf("task/ai-advice")),
                    ),
                    // Space permissions
                    Permission(
                        authorizedActions =
                            listOf("ship-ownership", "add-admin", "modify-admin", "remove-admin"),
                        authorizedResource = AuthorizedResource(types = listOf("space")),
                        customLogic = "owned",
                    ),
                    Permission(
                        authorizedActions = listOf("modify", "delete"),
                        authorizedResource = AuthorizedResource(types = listOf("space")),
                        customLogic = "is-space-admin",
                    ),
                    Permission(
                        authorizedActions = listOf("query", "create", "enumerate"),
                        authorizedResource = AuthorizedResource(types = listOf("space")),
                    ),
                    // Knowledge permissions
                    //                    Permission(
                    //                        authorizedActions =
                    //                            listOf("ship-ownership", "add-admin",
                    // "modify-admin", "remove-admin"),
                    //                        authorizedResource =
                    // AuthorizedResource(types =
                    // listOf("space")),
                    //                        customLogic = "owned",
                    //                    ),
                    Permission(
                        authorizedActions = listOf("create", "query", "query2", "delete", "update"),
                        authorizedResource = AuthorizedResource(types = listOf("knowledge")),
                    ),
                    // Team permissions
                    Permission(
                        authorizedActions =
                            listOf(
                                "ship-ownership",
                                "add-admin",
                                "remove-admin",
                                "modify-member",
                                "delete",
                            ),
                        authorizedResource = AuthorizedResource(types = listOf("team")),
                        customLogic = "owned",
                    ),
                    Permission(
                        authorizedActions =
                            listOf("add-normal-member", "remove-normal-member", "modify"),
                        authorizedResource = AuthorizedResource(types = listOf("team")),
                        customLogic = "is-team-admin",
                    ),
                    Permission(
                        authorizedActions = listOf("add-normal-member", "remove-normal-member"),
                        authorizedResource = AuthorizedResource(types = listOf("team")),
                        customLogic = "member-is-self",
                    ),
                    Permission(
                        authorizedActions =
                            listOf(
                                "query",
                                "create",
                                "enumerate",
                                "enumerate-my-teams",
                                "enumerate-members",
                            ),
                        authorizedResource = AuthorizedResource(types = listOf("team")),
                    ),

                    // Notification permissions
                    Permission(
                        authorizedActions =
                            listOf("list-notifications", "get-unread-count", "mark-as-read"),
                        authorizedResource = AuthorizedResource(types = listOf("notification")),
                    ),
                    Permission(
                        authorizedActions = listOf("delete"),
                        authorizedResource = AuthorizedResource(types = listOf("notification")),
                        customLogic = "is-notification-owner",
                    ),

                    // Project permissions
                    Permission(
                        authorizedActions = listOf("create", "enumerate", "update", "delete"),
                        authorizedResource = AuthorizedResource(types = listOf("project")),
                    ),

                    // Discussion permissions
                    Permission(
                        authorizedActions =
                            listOf("query-discussion", "create-discussion", "create-reaction"),
                        authorizedResource = AuthorizedResource(types = listOf("project")),
                    ),
                ),
        )
    }
}
