/*
 *  Description: This file implements the TaskService class.
 *               It is responsible for CRUD of a task.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *
 */

package org.rucca.cheese.task.service

import java.time.LocalDateTime
import java.time.ZoneId
import org.hibernate.query.SortDirection
import org.rucca.cheese.auth.services.AuthorizationQueryService
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.common.error.ForbiddenError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.error.PreconditionFailedError
import org.rucca.cheese.common.helper.EntityPatcher
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.pagination.model.TypedCompositeCursor
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.common.persistent.spec.div
import org.rucca.cheese.common.query.dsl.queryFor
import org.rucca.cheese.common.query.internal.spec.FullFilteringScope
import org.rucca.cheese.common.query.internal.spec.col
import org.rucca.cheese.common.query.internal.spec.exists
import org.rucca.cheese.common.query.internal.spec.parent
import org.rucca.cheese.common.query.model.QueryObject
import org.rucca.cheese.common.query.runtime.findWithQueryObject
import org.rucca.cheese.model.*
import org.rucca.cheese.model.TaskSubmitterTypeDTO.TEAM
import org.rucca.cheese.model.TaskSubmitterTypeDTO.USER
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.space.models.Space
import org.rucca.cheese.space.models.SpaceCategory
import org.rucca.cheese.space.repositories.SpaceCategoryRepository
import org.rucca.cheese.space.repositories.SpaceRepository
import org.rucca.cheese.task.*
import org.rucca.cheese.task.auth.TaskDomain
import org.rucca.cheese.task.auth.TaskResource
import org.rucca.cheese.task.auth.TaskRole
import org.rucca.cheese.task.option.TaskEnumerateOptions
import org.rucca.cheese.task.option.TaskQueryOptions
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.team.TeamUserRelation
import org.rucca.cheese.topic.Topic
import org.rucca.cheese.topic.TopicService
import org.rucca.cheese.user.User
import org.rucca.cheese.user.services.UserRealNameService
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskService(
    private val userService: UserService,
    private val teamService: TeamService,
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionService: TaskSubmissionService,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceCategoryRepository: SpaceCategoryRepository,
    private val spaceService: SpaceService,
    private val taskTopicsService: TaskTopicsService,
    private val taskMembershipService: TaskMembershipService,
    private val taskMembershipEligibilityService: TaskMembershipEligibilityService,
    private val taskMembershipViewService: TaskMembershipViewService,
    private val userRealNameService: UserRealNameService,
    private val entityPatcher: EntityPatcher,
    private val topicService: TopicService,
    private val authorizationQueryService: AuthorizationQueryService,
    private val taskNotificationService: TaskNotificationService,
) {
    private val logger = LoggerFactory.getLogger(TaskService::class.java)

    private enum class TaskOperationType(val presentTense: String, val pastTense: String) {
        MODIFY("modify", "modified"),
        DELETE("delete", "deleted"),
    }

    /**
     * Ensures that the task's current state allows the requested operation (Modify or Delete).
     * Based on the rule: If a task has participants or submissions, only Space Admins can modify or
     * delete it. Throws PreconditionFailedError if the operation is blocked by the state and user
     * role.
     *
     * @param taskId The ID of the task being operated on.
     * @param userId The ID of the user attempting the operation.
     * @param operationType The type of operation being attempted (MODIFY or DELETE).
     * @throws PreconditionFailedError if the task state prohibits the operation for the given user
     *   roles.
     */
    private fun ensureTaskStateAllowsOperation(
        taskId: IdType,
        userId: IdType,
        operationType: TaskOperationType,
    ) {
        // 1. Check the task's state (same as before)
        val hasParticipants = taskMembershipRepository.existsByTaskId(taskId)
        val hasSubmissions = taskSubmissionService.taskHasAnySubmission(taskId)
        val isOperationRestrictedByState = hasParticipants || hasSubmissions

        // 2. Apply the business rule using the AuthorizationQueryService
        if (isOperationRestrictedByState) {
            // Check if the user has the specific role needed to bypass the restriction
            val canBypassStateRestriction =
                authorizationQueryService.hasEffectiveRole(
                    userId,
                    TaskRole.SPACE_ADMIN, // The specific role needed
                    TaskDomain, // Domain
                    TaskResource.TASK, // Resource Type
                    taskId, // Resource ID
                )

            if (!canBypassStateRestriction) {
                val allUserRoles =
                    authorizationQueryService.getEffectiveRoles(
                        userId,
                        TaskDomain,
                        TaskResource.TASK,
                        taskId,
                    ) // Get roles for logging if needed
                val reasonCode = "TASK_STATE_RESTRICTS_${operationType.name}"
                val message =
                    "Task cannot be ${operationType.pastTense} because it has active participants or submissions. " +
                        "Only specific roles (e.g., Space Administrator) can perform this operation in the current state."
                logger.warn(
                    "User {} attempted to {} task {} but was blocked due to task state and effective roles {}.",
                    userId,
                    operationType.presentTense,
                    taskId,
                    allUserRoles.joinToString { it.roleId },
                )
                throw PreconditionFailedError(
                    message,
                    mapOf("taskId" to taskId, "reason" to reasonCode),
                )
            } else {
                logger.info(
                    "User {} (effectively has {}) is performing '{}' on task {} which has participants/submissions. Bypassing state restriction.",
                    userId,
                    TaskRole.SPACE_ADMIN.roleId,
                    operationType.presentTense,
                    taskId,
                )
            }
        }
    }

    fun getTaskDto(
        taskId: IdType,
        options: TaskQueryOptions = TaskQueryOptions.MINIMUM,
        currentUserId: IdType? = null,
    ): TaskDTO {
        val task = getTask(taskId)
        return task.toTaskDTO(options, currentUserId)
    }

    fun getTaskOwner(taskId: IdType): IdType {
        val task = getTask(taskId)
        return task.creator.id!!.toLong()
    }

    fun isTaskParticipant(taskId: IdType, userId: IdType, memberId: IdType): Boolean {
        return when (getTaskSumbitterType(taskId)) {
            USER ->
                userId == memberId &&
                    taskMembershipRepository.existsByTaskIdAndMemberId(taskId, memberId)

            TEAM ->
                teamService.isTeamMember(memberId, userId) &&
                    taskMembershipRepository.existsByTaskIdAndMemberId(taskId, memberId)
        }
    }

    fun isTaskApproved(taskId: IdType): Boolean {
        val task = getTask(taskId)
        return task.approved == ApproveType.APPROVED
    }

    fun isParticipantApproved(taskId: IdType, memberId: IdType): Boolean {
        val taskMembership = taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId)
        if (taskMembership.isPresent) {
            return when (taskMembership.get().approved!!) {
                ApproveType.APPROVED -> true
                else -> {
                    false
                }
            }
        }
        return false
    }

    fun taskHasAnyParticipant(taskId: IdType): Boolean {
        return taskMembershipRepository.existsByTaskId(taskId)
    }

    fun convertTaskSubmitterType(type: TaskSubmitterType): TaskSubmitterTypeDTO {
        return when (type) {
            TaskSubmitterType.USER -> USER
            TaskSubmitterType.TEAM -> TEAM
        }
    }

    fun convertTaskSubmitterType(type: TaskSubmitterTypeDTO): TaskSubmitterType {
        return when (type) {
            USER -> TaskSubmitterType.USER
            TEAM -> TaskSubmitterType.TEAM
        }
    }

    fun convertTaskSubmissionEntryType(type: TaskSubmissionEntryType): TaskSubmissionTypeDTO {
        return when (type) {
            TaskSubmissionEntryType.TEXT -> TaskSubmissionTypeDTO.TEXT
            TaskSubmissionEntryType.ATTACHMENT -> TaskSubmissionTypeDTO.FILE
        }
    }

    fun Task.toTaskDTO(options: TaskQueryOptions, currentUserId: IdType? = null): TaskDTO {
        val space =
            if (options.querySpace && this.space.id != null)
                spaceService.getSpaceDto(this.space.id!!, currentUserId = currentUserId)
            else null
        val category =
            if (options.querySpace && this.category.id != null)
                spaceService.getCategoryDTO(this.space.id!!, this.category.id!!)
            else null
        val participationEligibilityDto =
            if (options.queryJoinability && currentUserId != null) {
                taskMembershipService.getParticipationEligibility(this.id!!, currentUserId)
            } else null
        val submittability =
            if (options.querySubmittability && currentUserId != null)
                taskMembershipService.getSubmittability(this.id!!, currentUserId)
            else null
        val joined =
            if (options.queryJoined && currentUserId != null)
                taskMembershipService.getJoined(this.id!!, currentUserId)
            else null
        val userDeadline =
            if (options.queryUserDeadline && currentUserId != null)
                taskMembershipService.getUserDeadline(this.id!!, currentUserId)
            else null
        val topics =
            if (options.queryTopics) taskTopicsService.getTaskTopicDTOs(this.id!!) else null
        return TaskDTO(
            id = this.id!!,
            name = this.name,
            submitterType = convertTaskSubmitterType(this.submitterType),
            creator = userService.getUserDto(this.creator.id!!.toLong()),
            registrationStartAt =
                this.registrationStartAt
                    ?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
            deadline = this.deadline?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            participantLimit = this.participantLimit,
            defaultDeadline = this.defaultDeadline,
            resubmittable = this.resubmittable,
            editable = this.editable,
            intro = this.intro,
            description = this.description,
            space = space,
            category = category,
            submissionSchema =
                this.submissionSchema!!
                    .sortedBy { it.index }
                    .map {
                        TaskSubmissionSchemaEntryDTO(
                            it.description!!,
                            convertTaskSubmissionEntryType(it.type!!),
                        )
                    },
            participants = taskMembershipViewService.getTaskParticipantsSummary(this),
            updatedAt = this.updatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = this.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            participationEligibility = participationEligibilityDto,
            submittable = submittability?.first,
            submittableAsTeam = submittability?.second,
            rank = this.rank,
            approved = this.approved.convert(),
            rejectReason = this.rejectReason,
            joined = joined?.first,
            joinedTeams = joined?.second,
            topics = topics,
            requireRealName = this.requireRealName,
            userDeadline = userDeadline,
            minTeamSize = this.minTeamSize,
            maxTeamSize = this.maxTeamSize,
            teamLockingPolicy = this.teamLockingPolicy.toDTO(),
        )
    }

    private fun validateAndGetCategory(categoryId: IdType, spaceId: IdType): SpaceCategory {
        val categoryEntity =
            spaceCategoryRepository.findByIdAndSpaceId(categoryId, spaceId)
                ?: throw NotFoundError("Category not found or does not belong to space ${spaceId}.")

        if (categoryEntity.isArchived) {
            throw BadRequestError("Cannot assign task to an archived category (id=$categoryId).")
        }

        if (categoryEntity.deletedAt != null) {
            throw BadRequestError("Cannot assign task to a deleted category (id=$categoryId).")
        }

        return categoryEntity
    }

    fun createTask(
        name: String,
        submitterType: TaskSubmitterType,
        registrationStartAt: LocalDateTime?,
        deadline: LocalDateTime?,
        participantLimit: Int?,
        defaultDeadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<TaskSubmissionSchema>,
        creatorId: IdType,
        spaceId: IdType,
        categoryId: IdType?,
        rank: Int? = null,
        requireRealName: Boolean = false,
        minTeamSize: Int? = null,
        maxTeamSize: Int? = null,
        teamMembershipLockPolicy: TeamMembershipLockPolicy = TeamMembershipLockPolicy.NO_LOCK,
    ): IdType {
        if (
            submitterType != TaskSubmitterType.TEAM && (minTeamSize != null || maxTeamSize != null)
        ) {
            throw BadRequestError(
                "minTeamSize and maxTeamSize can only be set for TEAM type tasks."
            )
        }

        val spaceEntity = spaceService.getSpaceDto(spaceId, currentUserId = creatorId)

        val categoryEntity =
            if (categoryId != null) {
                // Validate the provided category ID (includes archived check)
                validateAndGetCategory(categoryId, spaceId)
            } else {
                // Use the space's default category
                validateAndGetCategory(spaceEntity.defaultCategoryId, spaceId)
            }

        val task =
            taskRepository.save(
                Task(
                    name = name,
                    submitterType = submitterType,
                    creator = userService.getUserReference(creatorId),
                    registrationStartAt = registrationStartAt,
                    deadline = deadline,
                    participantLimit = participantLimit,
                    defaultDeadline = defaultDeadline,
                    resubmittable = resubmittable,
                    editable = editable,
                    space = spaceRepository.getReferenceById(spaceId),
                    category = categoryEntity,
                    intro = intro,
                    description = description,
                    submissionSchema = submissionSchema,
                    rank = rank,
                    approved = ApproveType.NONE,
                    rejectReason = "",
                    requireRealName = requireRealName,
                    minTeamSize = minTeamSize,
                    maxTeamSize = maxTeamSize,
                    teamLockingPolicy = teamMembershipLockPolicy,
                )
            )

        val creatorName = userService.getUserDto(creatorId).username
        val payload =
            taskNotificationService.buildTaskPayload(
                taskId = task.id!!,
                taskName = task.name,
                spaceId = spaceId,
                spaceName = spaceEntity.name,
                actorId = creatorId,
                actorName = creatorName,
                extraFields = mapOf("taskCreatorId" to creatorId, "taskCreatorName" to creatorName),
            )
        taskNotificationService.publishToSpaceOwners(
            spaceId = spaceId,
            type = org.rucca.cheese.notification.models.NotificationType.TASK_PENDING_APPROVAL,
            payload = payload,
            actorId = creatorId,
        )
        return task.id!!
    }

    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    fun resubmitTaskForApproval(taskId: IdType, currentUserId: IdType): TaskDTO {
        val task = getTask(taskId)

        if (currentUserId != task.creator.id?.toLong()) {
            throw ForbiddenError("Only the task creator can resubmit the task for approval.")
        }

        if (task.approved != ApproveType.DISAPPROVED) {
            throw ForbiddenError("Task $taskId is not in a state that allows resubmission.")
        }

        task.approved = ApproveType.NONE
        task.rejectReason = ""
        val savedTask = taskRepository.save(task)
        val actorName = userService.getUserDto(currentUserId).username
        val payload =
            taskNotificationService.buildTaskPayload(
                taskId = savedTask.id!!,
                taskName = savedTask.name,
                spaceId = savedTask.space.id!!,
                spaceName = spaceService.getSpaceDto(savedTask.space.id!!).name,
                actorId = currentUserId,
                actorName = actorName,
                extraFields =
                    mapOf(
                        "taskCreatorId" to savedTask.creator.id!!.toLong(),
                        "taskCreatorName" to actorName,
                    ),
            )
        taskNotificationService.publishToSpaceOwners(
            spaceId = savedTask.space.id!!,
            type = org.rucca.cheese.notification.models.NotificationType.TASK_RESUBMITTED,
            payload = payload,
            actorId = currentUserId,
        )
        return savedTask.toTaskDTO(
            TaskQueryOptions(querySpace = true, queryTopics = true),
            currentUserId,
        )
    }

    /**
     * Patches a Task entity with non-null values from the request DTO. Follows the pattern:
     * Validate/Pre-fetch -> Patch -> Save. This ensures efficiency and atomicity within the
     * transaction.
     *
     * @param taskId The ID of the Task to update.
     * @param patchDto The DTO containing fields to update.
     * @param currentUserId The ID of the user performing the update (for context/permission checks
     *   if needed later).
     * @return The updated TaskDTO.
     * @throws NotFoundError If the task or related entities (like category) are not found.
     * @throws BadRequestError If validation fails (e.g., invalid category).
     */
    @Transactional
    fun patchTask(taskId: IdType, patchDto: PatchTaskRequestDTO, currentUserId: IdType): TaskDTO {
        ensureTaskStateAllowsOperation(
            taskId = taskId,
            userId = currentUserId,
            operationType = TaskOperationType.MODIFY,
        )

        val task = getTask(taskId)
        val previousApprovedStatus = task.approved

        // Validate and fetch the new category if provided
        val newCategory =
            patchDto.categoryId?.let { newCatId ->
                validateAndGetCategory(
                    newCatId,
                    task.space.id!!,
                ) // Ensure category is valid for the task's space
            }

        // Pre-validate topics if necessary (assuming taskTopicsService handles this)
        patchDto.topics?.forEach { topicId -> topicService.ensureTopicExists(topicId) }

        // --- Patch the entity ---
        // Use EntityPatcher to apply changes from DTO to the entity.
        // Handlers are used for fields requiring special logic (e.g., type conversion, related
        // entity updates).
        // Fields with matching names/types and no handler are patched automatically by default.
        val updatedTask =
            entityPatcher.patch(task, patchDto) {
                // Handle conversion from DTO enum to entity enum
                handle(PatchTaskRequestDTO::approved) { entity, value ->
                    entity.approved = value.convert()
                }

                // Handle conversion from Long timestamp to LocalDateTime for deadline
                handle(PatchTaskRequestDTO::deadline) { entity, value ->
                    // Note: This handler only runs if patchDto.deadline is NOT null.
                    // The case for setting deadline to null is handled *after* this block.
                    entity.deadline = value.toLocalDateTime()
                }

                handle(PatchTaskRequestDTO::registrationStartAt) { entity, value ->
                    entity.registrationStartAt = value.toLocalDateTime()
                }

                // Handle TaskSubmissionSchema update (includes conversion)
                handle(PatchTaskRequestDTO::submissionSchema) { entity, schemaEntries ->
                    entity.submissionSchema =
                        schemaEntries.withIndex().map { (index, entryDto) ->
                            TaskSubmissionSchema(
                                index = index,
                                description =
                                    entryDto.prompt, // Assuming DTO field name is 'prompt'
                                // Requires TaskSubmissionService or a local conversion method
                                type =
                                    taskSubmissionService.convertTaskSubmissionEntryType(
                                        entryDto.type
                                    ),
                            )
                        }
                }

                // Handle category update using the pre-fetched category
                handle(PatchTaskRequestDTO::categoryId) { entity, _
                    -> // The value (newCatId) was already used
                    if (
                        newCategory != null
                    ) { // Only update if categoryId was actually provided in the DTO
                        entity.category = newCategory
                    }
                }

                // Handle updating topics relation (potential flush point - keep handler simple)
                handle(PatchTaskRequestDTO::topics) { entity, topicIds ->
                    // This directly calls another service. Be mindful of potential side effects or
                    // flushes.
                    // Ideally, taskTopicsService.updateTaskTopics should be safe to call within
                    // this transaction phase.
                    taskTopicsService.updateTaskTopics(entity.id!!, topicIds)
                }

                handle(PatchTaskRequestDTO::teamLockingPolicy) { entity, value ->
                    entity.teamLockingPolicy = value.toEntity()
                }
            }

        // --- Post-Patch Adjustments ---
        // Handle specific cases like setting fields to null based on flags,
        // which might be complex for the generic patcher handlers.

        if (patchDto.hasDeadline == false) {
            updatedTask.deadline = null // Explicitly set deadline to null
        }

        if (patchDto.hasParticipantLimit == false) {
            updatedTask.participantLimit = null // Explicitly set participant limit to null
        }

        if (patchDto.hasRegistrationStart == false) {
            updatedTask.registrationStartAt = null
        }

        // --- Save the entity ---
        // Persist all accumulated changes with a single save operation.
        val savedTask = taskRepository.save(updatedTask)
        publishTaskApprovalNotifications(savedTask, previousApprovedStatus, currentUserId)

        // --- Return Result ---
        // Fetch the comprehensive DTO based on the final saved state.
        return savedTask.toTaskDTO(TaskQueryOptions.MAXIMUM, currentUserId)
    }

    private fun publishTaskApprovalNotifications(
        savedTask: Task,
        previousApprovedStatus: ApproveType,
        actorId: IdType,
    ) {
        if (savedTask.approved == previousApprovedStatus) {
            return
        }

        val type =
            when (savedTask.approved) {
                ApproveType.APPROVED ->
                    org.rucca.cheese.notification.models.NotificationType.TASK_APPROVED
                ApproveType.DISAPPROVED ->
                    org.rucca.cheese.notification.models.NotificationType.TASK_REJECTED
                else -> return
            }

        val actorName = userService.getUserDto(actorId).username
        val taskCreatorId = savedTask.creator.id!!.toLong()
        val taskCreatorName = userService.getUserDto(taskCreatorId).username
        val payload =
            taskNotificationService.buildTaskPayload(
                taskId = savedTask.id!!,
                taskName = savedTask.name,
                spaceId = savedTask.space.id!!,
                spaceName = spaceService.getSpaceDto(savedTask.space.id!!).name,
                actorId = actorId,
                actorName = actorName,
                extraFields =
                    buildMap {
                        put("taskCreatorId", taskCreatorId)
                        put("taskCreatorName", taskCreatorName)
                        put("rejectReason", savedTask.rejectReason ?: "")
                    },
            )
        taskNotificationService.publishNotification(
            recipientIds = setOf(taskCreatorId),
            type = type,
            payload = payload,
            actorId = actorId,
        )
    }

    enum class TasksSortBy {
        DEADLINE,
        CREATED_AT,
        UPDATED_AT,
    }

    fun getTaskSumbitterType(taskId: IdType): TaskSubmitterTypeDTO {
        val task = getTask(taskId)
        return convertTaskSubmitterType(task.submitterType)
    }

    fun getTaskSpaceId(taskId: IdType): IdType? {
        val task = getTask(taskId)
        return task.space.id
    }

    fun enumerateTasks(
        currentUserId: IdType,
        enumerateOptions: TaskEnumerateOptions,
        keywords: String?,
        pageSize: Int,
        pageStart: String?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        queryOptions: TaskQueryOptions,
    ): Pair<List<TaskDTO>, EncodedCursorPageDTO> {
        val effectivePageSize = pageSize.coerceIn(1, 100)
        val sanitizedKeywords = keywords?.trim()?.takeIf { it.isNotEmpty() }
        val queryObject =
            buildTaskQueryObject(
                currentUserId = currentUserId,
                options = enumerateOptions,
                keywords = sanitizedKeywords,
                sortBy = sortBy,
                sortOrder = sortOrder,
            )

        return executeTaskQuery(
            queryObject = queryObject,
            pageSize = effectivePageSize,
            pageStart = pageStart,
            queryOptions = queryOptions,
            currentUserId = currentUserId,
        )
    }

    private fun buildTaskQueryObject(
        currentUserId: IdType,
        options: TaskEnumerateOptions,
        keywords: String?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
    ): QueryObject<Task> {
        val sanitizedKeywords = keywords?.trim()?.takeIf { it.isNotEmpty() }
        val propertyDirection = sortOrder.toJpaDirection()
        val sortProperty =
            when (sortBy) {
                TasksSortBy.CREATED_AT -> Task::createdAt
                TasksSortBy.UPDATED_AT -> Task::updatedAt
                TasksSortBy.DEADLINE -> Task::deadline
            }

        return queryFor<Task> {
            id(Task::id)

            options.joined?.let { configure { distinct() } }

            filters {
                Task::space / Space::id eq options.space
                Task::category / SpaceCategory::id eq options.categoryId
                Task::approved eq options.approved
                Task::creator / User::id eq options.owner?.toInt()

                options.topics
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { topics ->
                        exists {
                            col(TaskTopicsRelation::task / Task::id) eq parent(Task::id)
                            TaskTopicsRelation::topic / Topic::id inList topics.map { it.toInt() }
                        }
                    }

                options.joined?.let { joined ->
                    fun FullFilteringScope<Task>.applyJoinedLogic() {
                        or {
                            and {
                                Task::submitterType eq TaskSubmitterType.USER
                                exists {
                                    col(TaskMembership::task / Task::id) eq parent(Task::id)
                                    col(TaskMembership::memberId) eq currentUserId
                                }
                            }
                            and {
                                Task::submitterType eq TaskSubmitterType.TEAM
                                exists {
                                    col(TeamUserRelation::user / User::id) eq currentUserId.toInt()
                                    exists {
                                        col(TaskMembership::task / Task::id) eq parent(Task::id)
                                        col(TaskMembership::memberId) eq
                                            parent(TeamUserRelation::team / Team::id)
                                    }
                                }
                            }
                        }
                    }
                    if (joined) applyJoinedLogic() else not { applyJoinedLogic() }
                }
            }

            sanitizedKeywords?.let { kw ->
                search {
                    bool {
                        should { match(Task::name, kw) boost 2 }
                        should { match(Task::intro, kw) }
                    }
                }

                sort {
                    val relevanceDirection =
                        if (sortOrder == SortDirection.ASCENDING) Sort.Direction.ASC
                        else Sort.Direction.DESC
                    relevance(relevanceDirection)
                    by(Task::id, Sort.Direction.ASC)
                }
            }
                ?: run {
                    sort {
                        by(Task::stateWeight, Sort.Direction.ASC)
                        by(sortProperty, propertyDirection)
                        by(Task::id, Sort.Direction.ASC)
                    }
                }
        }
    }

    private fun executeTaskQuery(
        queryObject: QueryObject<Task>,
        pageSize: Int,
        pageStart: String?,
        queryOptions: TaskQueryOptions,
        currentUserId: IdType,
    ): Pair<List<TaskDTO>, EncodedCursorPageDTO> {
        val cursor = decodeTaskCursor(pageStart)
        val page = taskRepository.findWithQueryObject(queryObject, cursor, pageSize)
        val dtos = page.content.map { it.toTaskDTO(queryOptions, currentUserId = currentUserId) }
        return Pair(dtos, page.pageInfo.toPageDTO())
    }

    private fun decodeTaskCursor(cursor: String?): TypedCompositeCursor<Task>? {
        if (cursor.isNullOrBlank()) return null

        return try {
            TypedCompositeCursor.decode(cursor)
        } catch (ex: Exception) {
            logger.debug("Failed to decode task cursor '{}': {}", cursor, ex.message)
            null
        }
    }

    fun deleteTask(taskId: IdType, currentUserId: IdType) {
        ensureTaskStateAllowsOperation(
            taskId = taskId,
            userId = currentUserId,
            operationType = TaskOperationType.DELETE,
        )

        val task = getTask(taskId)
        task.deletedAt = LocalDateTime.now()
        val participants = taskMembershipRepository.findAllByTaskId(taskId)
        for (participant in participants) {
            participant.deletedAt = LocalDateTime.now()
            val submissions = taskSubmissionRepository.findAllByMembershipId(participant.id!!)
            for (submission in submissions) {
                submission.deletedAt = LocalDateTime.now()
            }
            taskSubmissionRepository.saveAll(submissions)
        }
        taskMembershipRepository.saveAll(participants)
        taskRepository.save(task)
    }

    /**
     * Gets teams that can be used for a task based on filter criteria
     *
     * @param taskId The task ID
     * @param filter Filter type ("eligible" for teams that meet requirements, "all" for all user's
     *   admin teams)
     * @return List of extended team DTOs with real name verification status
     */
    fun getTeamsForTask(userId: IdType, taskId: IdType, filter: String): List<TeamSummaryDTO> {
        val task = getTask(taskId)
        if (task.submitterType != TaskSubmitterType.TEAM) {
            throw BadRequestError("Task $taskId does not support team participation.")
        }

        val userTeams = teamService.getTeamSummariesOfUser(userId)

        return userTeams.mapNotNull { team ->
            val (eligibility, memberDetails, allVerified) =
                taskMembershipEligibilityService.checkTeamEligibilityForTeamTask(task, team.id)

            val addRealNameDetails =
                filter == "all" ||
                    (task.requireRealName &&
                        eligibility.reasons?.any {
                            it.code == EligibilityRejectReasonCodeDTO.TEAM_MEMBER_MISSING_REAL_NAME
                        } == true)

            val updatedSummary =
                team.copy(
                    allMembersVerified = allVerified,
                    memberRealNameStatus =
                        if (addRealNameDetails)
                            memberDetails?.map {
                                TeamMemberRealNameStatusDTO(
                                    it.user.id,
                                    it.hasRealNameInfo == true,
                                    it.user.nickname,
                                )
                            }
                        else null,
                )

            when (filter) {
                "eligible" -> if (eligibility.eligible) updatedSummary else null
                "all" -> updatedSummary
                else -> null
            }
        }
    }

    /**
     * Enables the 'requireRealName' flag for a specific task. Pre-checks: Verifies that all current
     * participants (if any approved) have real name info.
     *
     * @param taskId The ID of the task to modify.
     * @throws NotFoundError if the task doesn't exist.
     * @throws PreconditionFailedError if the task is already set to require real name, or if
     *   enabling it would violate constraints (e.g., participants missing real name info).
     */
    @Transactional // Ensure atomicity of checks and update
    fun enableRealNameRequirement(taskId: IdType) {
        val task = getTask(taskId) // Fetches task or throws NotFoundError

        if (task.requireRealName) {
            throw PreconditionFailedError(
                "Task $taskId already requires real name.",
                mapOf("taskId" to taskId),
            )
        }

        // --- Pre-check: Ensure all *current approved* participants can satisfy the requirement ---
        // This check prevents enabling the flag if data is missing, which would break things later.
        val approvedMemberships =
            taskMembershipRepository.findAllByTaskIdAndApproved(taskId, ApproveType.APPROVED)
        if (approvedMemberships.isNotEmpty()) {
            // Check depends on submitter type
            when (task.submitterType) {
                TaskSubmitterType.USER -> {
                    val missingUsers =
                        approvedMemberships
                            .mapNotNull { it.memberId }
                            .filter { userId ->
                                !userRealNameService.hasUserIdentity(userId)
                            } // Check if user has real name registered
                    if (missingUsers.isNotEmpty()) {
                        throw PreconditionFailedError(
                            "Cannot enable real name requirement: The following approved users are missing real name info: ${missingUsers.joinToString()}",
                            mapOf("taskId" to taskId, "missingUserIds" to missingUsers),
                        )
                    }
                }

                TaskSubmitterType.TEAM -> {
                    val teamsWithMissingMembers =
                        approvedMemberships
                            .mapNotNull { it.memberId } // Get team IDs
                            .mapNotNull { teamId ->
                                val (_, allVerified) =
                                    teamService.getTeamMembers(
                                        teamId,
                                        queryRealNameStatus = true,
                                    ) // Check current members' status
                                if (allVerified != true) teamId
                                else null // Return teamId if any member is unverified
                            }
                    if (teamsWithMissingMembers.isNotEmpty()) {
                        throw PreconditionFailedError(
                            "Cannot enable real name requirement: One or more members in the following approved teams are missing real name info: ${teamsWithMissingMembers.joinToString()}",
                            mapOf(
                                "taskId" to taskId,
                                "teamsWithMissingMembers" to teamsWithMissingMembers,
                            ),
                        )
                    }
                }
            }
        }

        // --- Update the flag ---
        task.requireRealName = true
        taskRepository.save(task)

        // Note: This does NOT automatically backfill/encrypt snapshots for existing participants.
        // The `fixMissingRealNameInfo` MBean operation should be used for that if needed.
        logger.info("Enabled 'requireRealName' for Task ID: {}", taskId)
    }

    /**
     * Disables the 'requireRealName' flag for a specific task. This simply changes the flag; it
     * does not remove existing encrypted real name data.
     *
     * @param taskId The ID of the task to modify.
     * @throws NotFoundError if the task doesn't exist.
     * @throws PreconditionFailedError if the task is already set to not require real name.
     */
    @Transactional
    fun disableRealNameRequirement(taskId: IdType) {
        val task = getTask(taskId) // Fetches task or throws NotFoundError

        if (!task.requireRealName) {
            throw PreconditionFailedError(
                "Task $taskId does not require real name.",
                mapOf("taskId" to taskId),
            )
        }

        // --- Update the flag ---
        task.requireRealName = false
        // Consider if encryptionKeyId should be nulled? Safer to leave it.
        // Existing snapshots with encrypted data remain, but are no longer strictly required by the
        // flag.
        taskRepository.save(task)
        logger.info("Disabled 'requireRealName' for Task ID: {}", taskId)
    }
}
