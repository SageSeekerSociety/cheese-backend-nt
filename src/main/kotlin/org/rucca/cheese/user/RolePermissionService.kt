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
                    // Space permissions
                    Permission(
                        authorizedActions =
                            listOf("ship-ownership", "add-admin", "modify-admin", "remove-admin"),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("space"),
                            ),
                        customLogic = "owned",
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

                    // Team permissions
                    Permission(
                        authorizedActions =
                            listOf(
                                "ship-ownership",
                                "add-admin",
                                "remove-admin",
                                "modify-member",
                                "delete"
                            ),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("team"),
                            ),
                        customLogic = "owned",
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
                        authorizedActions = listOf("add-normal-member", "remove-normal-member"),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("team"),
                            ),
                        customLogic = "member-is-self",
                    ),
                    Permission(
                        authorizedActions =
                            listOf(
                                "query",
                                "create",
                                "enumerate",
                                "enumerate-my-teams",
                                "enumerate-members"
                            ),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("team"),
                            ),
                    ),

                    // Task permissions
                    Permission(
                        authorizedActions =
                            listOf(
                                "enumerate-submissions",
                                "add-participant",
                                "remove-participant",
                            ),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                        customLogic = "owned || is-space-admin-of-task || is-team-admin-of-task",
                    ),
                    Permission(
                        authorizedActions =
                            listOf(
                                "modify",
                                "delete",
                            ),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                        customLogic =
                            "(owned && ((!is-task-in-space && !is-task-in-team) || (!task-has-any-submission && !task-has-any-participant))) " +
                                "|| is-space-admin-of-task || is-team-admin-of-task",
                    ),
                    Permission(
                        authorizedActions =
                            listOf(
                                "modify-approved",
                            ),
                        authorizedResource = AuthorizedResource(types = listOf("task")),
                        customLogic = "is-space-admin-of-task || is-team-admin-of-task"
                    ),
                    Permission(
                        authorizedActions =
                            listOf(
                                "create-submission-review",
                                "modify-submission-review",
                                "delete-submission-review"
                            ),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                        customLogic = "is-task-owner-of-submission",
                    ),
                    Permission(
                        authorizedActions =
                            listOf("submit", "modify-submission", "enumerate-submissions"),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                        customLogic = "is-task-participant",
                    ),
                    Permission(
                        authorizedActions = listOf("add-participant", "remove-participant"),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                        customLogic =
                            "(is-user-task && task-member-is-self) || (is-team-task && task-user-is-admin-of-member)",
                    ),
                    Permission(
                        authorizedActions = listOf("create"),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                    ),
                    Permission(
                        authorizedActions = listOf("query", "enumerate", "enumerate-participants"),
                        authorizedResource =
                            AuthorizedResource(
                                types = listOf("task"),
                            ),
                        customLogic =
                            "is-task-approved || is-space-admin-of-task || is-team-admin-of-task"
                    ),
                )
        )
    }
}
