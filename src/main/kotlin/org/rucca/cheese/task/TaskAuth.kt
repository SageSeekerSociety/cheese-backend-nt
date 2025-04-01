package org.rucca.cheese.task

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
import org.rucca.cheese.model.ApproveTypeDTO
import org.rucca.cheese.model.TaskSubmitterTypeDTO
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.team.TeamService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
object TaskDomain : Domain {
    override val name: String = "task"
}

enum class TaskAction(override val actionId: String) : Action {
    CREATE("create"),
    QUERY("query"),
    ENUMERATE("enumerate"),
    MODIFY("modify"),
    DELETE("delete");

    override val domain: Domain = TaskDomain
}

enum class TaskResource(override val typeName: String) : ResourceType {
    TASK("task"),
    PARTICIPANT("participant"),
    SUBMISSION("submission"),
    SUBMISSION_REVIEW("submission-review"),
    AI_ADVICE("ai-advice");

    override val domain: Domain = TaskDomain

    companion object {
        fun of(typeName: String) =
            entries.find { it.typeName == typeName } ?: error("Invalid resource type")
    }
}

enum class TaskRole(override val roleId: String) : Role {
    OWNER("owner"),
    SPACE_ADMIN("space-admin"),
    PARTICIPANT("participant");

    override val domain: Domain? = TaskDomain
}

@Component
class TaskRoleHierarchyConfig(private val roleHierarchy: GraphRoleHierarchy) {
    @PostConstruct
    fun configureRoleHierarchy() {
        val hierarchyConfig = defineRoleHierarchy {
            role(TaskRole.OWNER)
            role(TaskRole.SPACE_ADMIN)
            role(TaskRole.PARTICIPANT)
        }

        roleHierarchy.applyHierarchy(hierarchyConfig)
    }
}

object TaskContextKeys {
    val TASK_ID = ContextKey.of<IdType>("taskId")
    val SPACE_ID = ContextKey.of<IdType>("spaceId")
    val QUERY_OWNER_ID = ContextKey.of<IdType>("queryOwner")
    val QUERY_JOINED = ContextKey.of<Boolean>("queryJoined")
    val MEMBER_ID = ContextKey.of<IdType>("memberId")
    val PARTICIPANT_ID = ContextKey.of<IdType>("participantId")
    val SUBMISSION_ID = ContextKey.of<IdType>("submissionId")
    val APPROVED = ContextKey.of<ApproveTypeDTO>("approved")
    val REJECT_REASON = ContextKey.of<String>("rejectReason")
    val SUBMITTER_TYPE = ContextKey.of<TaskSubmitterTypeDTO>("submitterType")
    val CONVERSATION_ID = ContextKey.of<String>("conversationId")
    val TASK_PARTICIPANT_DEADLINE = ContextKey.of<Long>("deadline")

    val GET_HAS_ANY_PARTICIPANT = ContextKey.of<(IdType) -> Boolean>("getHasAnyParticipant")
    val GET_HAS_ANY_SUBMISSION = ContextKey.of<(IdType) -> Boolean>("getHasAnySubmission")
    val GET_IS_TASK_APPROVED = ContextKey.of<(IdType) -> Boolean>("getIsTaskApproved")
    val GET_IS_PARTICIPANT_APPROVED =
        ContextKey.of<(IdType, IdType) -> Boolean>("getIsParticipantApproved")
    val IS_TEAM_AT_LEAST_ADMIN_PROVIDER =
        ContextKey.of<(IdType, IdType) -> Boolean>("isTeamAtLeastAdminProvider")
    val MEMBER_ID_PROVIDER = ContextKey.of<(IdType) -> IdType>("memberIdProvider")
}

@Component
class TaskContextProvider(
    private val taskService: TaskService,
    private val taskSubmissionService: TaskSubmissionService,
    private val teamService: TeamService,
    private val taskMembershipService: TaskMembershipService,
) : PermissionContextProvider {
    override val domain: Domain = TaskDomain

    override fun getContext(resourceName: String, resourceId: IdType?) =
        buildResourceContext(domain, TaskResource.of(resourceName), resourceId) {
            if (resourceId != null) {
                // First extract the task ID associated with any resource type
                val taskId =
                    when (TaskResource.of(resourceName)) {
                        TaskResource.TASK -> resourceId
                        TaskResource.PARTICIPANT,
                        TaskResource.SUBMISSION,
                        TaskResource.SUBMISSION_REVIEW,
                        TaskResource.AI_ADVICE -> {
                            resourceId
                        }
                    }

                TaskContextKeys.TASK_ID(taskId)

                val spaceId = taskService.getTaskSpaceId(taskId)
                if (spaceId != null) {
                    TaskContextKeys.SPACE_ID(spaceId)
                }

                val submitterType = taskService.getTaskSumbitterType(taskId)
                TaskContextKeys.SUBMITTER_TYPE(submitterType)

                TaskContextKeys.GET_HAS_ANY_PARTICIPANT { id ->
                    taskService.taskHasAnyParticipant(id)
                }

                TaskContextKeys.GET_HAS_ANY_SUBMISSION { id ->
                    taskSubmissionService.taskHasAnySubmission(id)
                }

                TaskContextKeys.GET_IS_TASK_APPROVED { id -> taskService.isTaskApproved(id) }

                TaskContextKeys.GET_IS_PARTICIPANT_APPROVED { tId, mId ->
                    taskService.isParticipantApproved(tId, mId)
                }

                TaskContextKeys.IS_TEAM_AT_LEAST_ADMIN_PROVIDER { teamId, userId ->
                    teamService.isTeamAtLeastAdmin(teamId, userId)
                }

                TaskContextKeys.MEMBER_ID_PROVIDER { participantId ->
                    taskMembershipService.getTaskParticipantMemberId(participantId)
                }
            }
        }
}

@Component
class TaskRoleProvider(
    private val taskService: TaskService,
    private val taskMembershipService: TaskMembershipService,
    private val spaceService: SpaceService,
) : DomainRoleProvider {
    private val logger = LoggerFactory.getLogger(TaskRoleProvider::class.java)

    override val domain: Domain = TaskDomain

    override fun getRoles(userId: IdType, context: Map<String, Any>): Set<Role> {
        val roles = mutableSetOf<Role>()
        val taskId =
            DomainContextKeys.RESOURCE_ID.get(context) ?: TaskContextKeys.TASK_ID.get(context)
        val memberId = TaskContextKeys.MEMBER_ID.get(context)

        // Check space admin role
        val spaceId = TaskContextKeys.SPACE_ID.get(context)
        if (spaceId != null && spaceService.isSpaceAdmin(spaceId, userId)) {
            roles.add(TaskRole.SPACE_ADMIN)
        }

        if (taskId != null) {
            // Check if user is task owner
            if (taskService.getTaskOwner(taskId) == userId) {
                roles.add(TaskRole.OWNER)
            }

            // Check space admin role
            if (spaceId == null) {
                val taskSpaceId = taskService.getTaskSpaceId(taskId)
                if (taskSpaceId != null && spaceService.isSpaceAdmin(taskSpaceId, userId)) {
                    roles.add(TaskRole.SPACE_ADMIN)
                }
            }

            // Check participant role
            if (memberId != null) {
                if (taskService.isTaskParticipant(taskId, userId, memberId)) {
                    roles.add(TaskRole.PARTICIPANT)
                }
            } else if (taskMembershipService.isTaskParticipant(taskId, userId)) {
                roles.add(TaskRole.PARTICIPANT)
            }
        }

        logger.info("User $userId has roles: $roles in task $taskId")

        return roles
    }
}

@Component
class TaskPermissionConfig(
    private val permissionService: PermissionConfigurationService,
    private val registrationService: RegistrationService,
) : DomainPermissionService {
    private val logger = LoggerFactory.getLogger(TaskPermissionConfig::class.java)

    override val domain: Domain = TaskDomain

    @PostConstruct
    override fun configurePermissions() {
        // Register actions and resources
        registrationService.registerActions(*TaskAction.entries.toTypedArray())
        registrationService.registerResources(*TaskResource.entries.toTypedArray())

        // Define permissions
        val config = definePermissions {
            // User role permissions
            role(SystemRole.USER) {
                can(TaskAction.CREATE).on(TaskResource.TASK).all()

                can(TaskAction.ENUMERATE).on(TaskResource.TASK).where {
                    withCondition { (userId, _), _, _, _, context ->
                        val queryOwner = TaskContextKeys.QUERY_OWNER_ID.get(context)
                        val queryJoined = TaskContextKeys.QUERY_JOINED.get(context)
                        val approveType = TaskContextKeys.APPROVED.get(context)

                        queryOwner == userId ||
                            queryJoined == true ||
                            approveType == ApproveTypeDTO.APPROVED
                    }
                }

                can(TaskAction.QUERY).on(TaskResource.TASK).where {
                    withCondition { _, _, _, resourceId, context ->
                        val taskId = resourceId ?: return@withCondition false
                        val getIsTaskApproved =
                            TaskContextKeys.GET_IS_TASK_APPROVED.get(context)
                                ?: return@withCondition false

                        getIsTaskApproved(taskId)
                    }
                }

                can(TaskAction.CREATE).on(TaskResource.PARTICIPANT).where {
                    withCondition { userInfo, _, _, _, context ->
                        val deadline = TaskContextKeys.TASK_PARTICIPANT_DEADLINE.get(context)
                        if (deadline != null) {
                            return@withCondition false
                        }
                        val memberId =
                            TaskContextKeys.MEMBER_ID.get(context) ?: return@withCondition false
                        val submitterType =
                            TaskContextKeys.SUBMITTER_TYPE.get(context)
                                ?: return@withCondition false
                        val getIsTeamAtLeastAdmin =
                            TaskContextKeys.IS_TEAM_AT_LEAST_ADMIN_PROVIDER.get(context)
                                ?: return@withCondition false

                        (submitterType == TaskSubmitterTypeDTO.USER &&
                            userInfo.userId == memberId) ||
                            (submitterType == TaskSubmitterTypeDTO.TEAM &&
                                getIsTeamAtLeastAdmin(memberId, userInfo.userId))
                    }
                }

                can(TaskAction.QUERY, TaskAction.CREATE, TaskAction.DELETE)
                    .on(TaskResource.AI_ADVICE)
                    .all()
            }

            // Task owner permissions
            role(TaskRole.OWNER) {
                can(TaskAction.QUERY, TaskAction.ENUMERATE)
                    .on(TaskResource.TASK, TaskResource.PARTICIPANT)
                    .all()

                can(TaskAction.DELETE).on(TaskResource.TASK).where {
                    withCondition { userInfo, action, resourceType, resourceId, context ->
                        val hasAnyParticipant =
                            TaskContextKeys.GET_HAS_ANY_PARTICIPANT.get(context)
                                ?: return@withCondition false
                        val hasAnySubmission =
                            TaskContextKeys.GET_HAS_ANY_SUBMISSION.get(context)
                                ?: return@withCondition false

                        // Only allow deletion if there are no participants or submissions
                        if (hasAnyParticipant(resourceId!!) || hasAnySubmission(resourceId)) {
                            return@withCondition false
                        }

                        true
                    }
                }

                can(TaskAction.MODIFY, TaskAction.DELETE).on(TaskResource.PARTICIPANT).all()

                can(TaskAction.MODIFY).on(TaskResource.TASK).where {
                    withCondition { _, _, _, resourceId, context ->
                        val taskId = resourceId ?: return@withCondition false
                        val getHasAnyParticipant =
                            TaskContextKeys.GET_HAS_ANY_PARTICIPANT.get(context)
                                ?: return@withCondition false
                        val getHasAnySubmission =
                            TaskContextKeys.GET_HAS_ANY_SUBMISSION.get(context)
                                ?: return@withCondition false

                        val approved = TaskContextKeys.APPROVED.get(context)
                        val rejectReason = TaskContextKeys.REJECT_REASON.get(context)

                        if ((approved != null || rejectReason != null)) {
                            return@withCondition false
                        }

                        !getHasAnyParticipant(taskId) && !getHasAnySubmission(taskId)
                    }
                }

                can(TaskAction.MODIFY).on(TaskResource.PARTICIPANT).all()

                can(TaskAction.CREATE, TaskAction.MODIFY, TaskAction.DELETE)
                    .on(TaskResource.SUBMISSION_REVIEW)
                    .all()

                can(TaskAction.ENUMERATE).on(TaskResource.SUBMISSION).all()

                can(TaskAction.CREATE).on(TaskResource.PARTICIPANT).where {
                    withCondition { userInfo, _, _, _, context ->
                        val deadline = TaskContextKeys.TASK_PARTICIPANT_DEADLINE.get(context)
                        deadline != null
                    }
                }
            }

            // Space admin permissions
            role(TaskRole.SPACE_ADMIN) {
                can(TaskAction.QUERY, TaskAction.MODIFY, TaskAction.DELETE, TaskAction.ENUMERATE)
                    .on(TaskResource.TASK)
                    .all()

                can(TaskAction.ENUMERATE, TaskAction.QUERY).on(TaskResource.PARTICIPANT).all()
            }

            // Participant permissions
            role(TaskRole.PARTICIPANT) {
                can(TaskAction.QUERY).on(TaskResource.TASK).all()

                can(TaskAction.CREATE, TaskAction.MODIFY).on(TaskResource.SUBMISSION).where {
                    withCondition { userInfo, action: TaskAction, _, _, context ->
                        val taskId =
                            TaskContextKeys.TASK_ID.get(context) ?: return@withCondition false
                        val submitterType =
                            TaskContextKeys.SUBMITTER_TYPE.get(context)
                                ?: return@withCondition false
                        val ctxMemberId = TaskContextKeys.MEMBER_ID.get(context)
                        val memberId =
                            if (ctxMemberId == null) {
                                val participantId =
                                    TaskContextKeys.PARTICIPANT_ID.get(context)
                                        ?: return@withCondition false
                                val getMemberId =
                                    TaskContextKeys.MEMBER_ID_PROVIDER.get(context)
                                        ?: return@withCondition false
                                getMemberId(participantId)
                            } else ctxMemberId
                        val getIsParticipantApproved =
                            TaskContextKeys.GET_IS_PARTICIPANT_APPROVED.get(context)
                                ?: return@withCondition false

                        // 检查参与者是否被批准
                        if (!getIsParticipantApproved(taskId, memberId)) {
                            return@withCondition false
                        }

                        // 如果是个人任务，直接检查用户ID
                        if (submitterType == TaskSubmitterTypeDTO.USER) {
                            return@withCondition userInfo.userId == memberId
                        }

                        // 如果是团队任务，检查用户是否是团队管理员
                        val getIsTeamAtLeastAdmin =
                            TaskContextKeys.IS_TEAM_AT_LEAST_ADMIN_PROVIDER.get(context)
                                ?: return@withCondition false
                        getIsTeamAtLeastAdmin(memberId, userInfo.userId)
                    }
                }

                can(TaskAction.ENUMERATE).on(TaskResource.SUBMISSION).where {
                    withCondition { userInfo, _, _, _, context ->
                        val submitterType =
                            TaskContextKeys.SUBMITTER_TYPE.get(context)
                                ?: return@withCondition false
                        val ctxMemberId = TaskContextKeys.MEMBER_ID.get(context)
                        val memberId =
                            if (ctxMemberId == null) {
                                val participantId =
                                    TaskContextKeys.PARTICIPANT_ID.get(context)
                                        ?: return@withCondition false
                                val getMemberId =
                                    TaskContextKeys.MEMBER_ID_PROVIDER.get(context)
                                        ?: return@withCondition false
                                getMemberId(participantId)
                            } else ctxMemberId

                        // 如果是个人任务，检查用户ID是否匹配
                        if (submitterType == TaskSubmitterTypeDTO.USER) {
                            return@withCondition userInfo.userId == memberId
                        }

                        // 如果是团队任务，检查用户是否是团队成员
                        val getIsTeamAtLeastAdmin =
                            TaskContextKeys.IS_TEAM_AT_LEAST_ADMIN_PROVIDER.get(context)
                                ?: return@withCondition false
                        getIsTeamAtLeastAdmin(memberId, userInfo.userId)
                    }
                }

                can(TaskAction.MODIFY, TaskAction.DELETE).on(TaskResource.PARTICIPANT).where {
                    withCondition { userInfo, action: TaskAction, _, _, context ->
                        val submitterType =
                            TaskContextKeys.SUBMITTER_TYPE.get(context)
                                ?: return@withCondition false
                        val getIsTeamAtLeastAdmin =
                            TaskContextKeys.IS_TEAM_AT_LEAST_ADMIN_PROVIDER.get(context)
                                ?: return@withCondition false

                        val approved = TaskContextKeys.APPROVED.get(context)

                        if (action == TaskAction.MODIFY && approved != null) {
                            return@withCondition false
                        }

                        val ctxMemberId = TaskContextKeys.MEMBER_ID.get(context)
                        val memberId =
                            if (ctxMemberId == null) {
                                val participantId =
                                    TaskContextKeys.PARTICIPANT_ID.get(context)
                                        ?: return@withCondition false
                                val getMemberId =
                                    TaskContextKeys.MEMBER_ID_PROVIDER.get(context)
                                        ?: return@withCondition false
                                getMemberId(participantId)
                            } else ctxMemberId
                        (submitterType == TaskSubmitterTypeDTO.USER &&
                            userInfo.userId == memberId) ||
                            (submitterType == TaskSubmitterTypeDTO.TEAM &&
                                getIsTeamAtLeastAdmin(memberId, userInfo.userId))
                    }
                }
            }
        }

        // Apply configuration
        permissionService.applyConfiguration(config)
    }
}
