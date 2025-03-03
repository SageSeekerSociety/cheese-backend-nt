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

                    // Task permissions
                    Permission(
                        authorizedActions =
                            listOf(
                                "enumerate-submissions",
                                "remove-participant",
                                "modify-membership",
                                "query-participant-real-name-info",
                            ),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic = "owned || is-space-admin-of-task || is-team-admin-of-task",
                    ),
                    Permission(
                        authorizedActions = listOf("modify", "delete"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic =
                            "(owned && ((!is-task-in-space && !is-task-in-team) || (!task-has-any-submission && !task-has-any-participant))) " +
                                "|| is-space-admin-of-task || is-team-admin-of-task",
                    ),
                    Permission(
                        authorizedActions = listOf("modify-approved", "modify-reject-reason"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic =
                            "is-space-admin-of-task || is-team-admin-of-task || (owned && is-modifying-approved-to-none)",
                    ),
                    Permission(
                        authorizedActions =
                            listOf(
                                "create-submission-review",
                                "modify-submission-review",
                                "delete-submission-review",
                            ),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic = "is-task-owner-of-submission",
                    ),
                    Permission(
                        authorizedActions =
                            listOf("submit", "modify-submission", "enumerate-submissions"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic = "is-task-participant && is-participant-approved",
                    ),
                    Permission(
                        authorizedActions = listOf("remove-participant"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic =
                            "(is-user-task && task-member-is-self) || (is-team-task && task-user-is-admin-of-member)",
                    ),
                    Permission(
                        authorizedActions = listOf("add-participant"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic =
                            "(!deadline-is-set && is-task-approved && ((is-user-task && task-member-is-self) || (is-team-task && task-user-is-admin-of-member)))" +
                                "|| (deadline-is-set && (owned || is-space-admin-of-task || is-team-admin-of-task))",
                    ),
                    Permission(
                        authorizedActions = listOf("create"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                    ),
                    Permission(
                        authorizedActions = listOf("query", "enumerate", "enumerate-participants"),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic =
                            "is-task-approved || (owned || is-enumerating-owned-tasks) " +
                                "|| is-space-admin-of-task || is-team-admin-of-task",
                    ),
                ),
        )
    }
}
