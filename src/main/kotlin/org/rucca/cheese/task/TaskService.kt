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

package org.rucca.cheese.task

import jakarta.persistence.criteria.*
import org.hibernate.query.SortDirection
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.repository.findAllWithIdCursor
import org.rucca.cheese.common.pagination.repository.idSeekSpec
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.*
import org.rucca.cheese.model.TaskSubmitterTypeDTO.TEAM
import org.rucca.cheese.model.TaskSubmitterTypeDTO.USER
import org.rucca.cheese.space.Space
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.task.option.TaskEnumerateOptions
import org.rucca.cheese.task.option.TaskQueryOptions
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.team.TeamUserRelation
import org.rucca.cheese.topic.Topic
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TaskService(
    private val userService: UserService,
    private val teamService: TeamService,
    private val authenticationService: AuthenticationService,
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val elasticsearchTemplate: ElasticsearchTemplate,
    private val spaceService: SpaceService,
    private val taskTopicsService: TaskTopicsService,
    private val taskMembershipService: TaskMembershipService,
) {
    fun getTaskDto(taskId: IdType, options: TaskQueryOptions = TaskQueryOptions.MINIMUM): TaskDTO {
        val task = getTask(taskId)
        return task.toTaskDTO(options)
    }

    fun getTaskOwner(taskId: IdType): IdType {
        val task = getTask(taskId)
        return task.creator!!.id!!.toLong()
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

    fun Task.toTaskDTO(options: TaskQueryOptions): TaskDTO {
        val userId = authenticationService.getCurrentUserId()
        val space =
            if (options.querySpace && this.space?.id != null)
                spaceService.getSpaceDto(this.space.id!!)
            else null
        val team =
            if (options.queryTeam && this.team?.id != null) teamService.getTeamDto(this.team.id!!)
            else null
        val joinability =
            if (options.queryJoinability) taskMembershipService.getJoinability(this, userId)
            else Pair(null, null)
        val submittability =
            if (options.querySubmittability) taskMembershipService.getSubmittability(this, userId)
            else Pair(null, null)
        val joined =
            if (options.queryJoined) taskMembershipService.getJoined(this, userId)
            else Pair(null, null)
        val joinedApproved =
            if (options.queryJoinedApproved)
                taskMembershipService.getJoinedWithApproveType(this, userId, ApproveType.APPROVED)
            else Pair(null, null)
        val joinedDisapproved =
            if (options.queryJoinedDisapproved)
                taskMembershipService.getJoinedWithApproveType(
                    this,
                    userId,
                    ApproveType.DISAPPROVED,
                )
            else Pair(null, null)
        val joinedNotApprovedOrDisapproved =
            if (options.queryJoinedNotApprovedOrDisapproved)
                taskMembershipService.getJoinedWithApproveType(this, userId, ApproveType.NONE)
            else Pair(null, null)
        val topics =
            if (options.queryTopics) taskTopicsService.getTaskTopicDTOs(this.id!!) else null
        return TaskDTO(
            id = this.id!!,
            name = this.name!!,
            submitterType = convertTaskSubmitterType(this.submitterType!!),
            creator = userService.getUserDto(this.creator!!.id!!.toLong()),
            deadline = this.deadline?.toEpochMilli(),
            participantLimit = this.participantLimit,
            defaultDeadline = this.defaultDeadline!!,
            resubmittable = this.resubmittable!!,
            editable = this.editable!!,
            intro = this.intro!!,
            description = this.description!!,
            space = space,
            team = team,
            submissionSchema =
                this.submissionSchema!!
                    .sortedBy { it.index }
                    .map {
                        TaskSubmissionSchemaEntryDTO(
                            it.description!!,
                            convertTaskSubmissionEntryType(it.type!!),
                        )
                    },
            submitters = getTaskSubmittersSummary(this.id!!),
            updatedAt = this.updatedAt!!.toEpochMilli(),
            createdAt = this.createdAt!!.toEpochMilli(),
            joinable = joinability.first,
            joinableAsTeam = joinability.second,
            submittable = submittability.first,
            submittableAsTeam = submittability.second,
            rank = this.rank,
            approved = this.approved!!.convert(),
            rejectReason = this.rejectReason,
            joined = joined.first,
            joinedAsTeam = joined.second,
            joinedApproved = joinedApproved.first,
            joinedApprovedAsTeam = joinedApproved.second,
            joinedDisapproved = joinedDisapproved.first,
            joinedDisapprovedAsTeam = joinedDisapproved.second,
            joinedNotApprovedOrDisapproved = joinedNotApprovedOrDisapproved.first,
            joinedNotApprovedOrDisapprovedAsTeam = joinedNotApprovedOrDisapproved.second,
            topics = topics,
        )
    }

    fun createTask(
        name: String,
        submitterType: TaskSubmitterType,
        deadline: LocalDateTime?,
        participantLimit: Int?,
        defaultDeadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<TaskSubmissionSchema>,
        creatorId: IdType,
        teamId: IdType?,
        spaceId: IdType?,
        rank: Int? = null,
    ): IdType {
        val task =
            taskRepository.save(
                Task(
                    name = name,
                    submitterType = submitterType,
                    creator = User().apply { id = creatorId.toInt() },
                    deadline = deadline,
                    participantLimit = participantLimit,
                    defaultDeadline = defaultDeadline,
                    resubmittable = resubmittable,
                    editable = editable,
                    team = if (teamId != null) Team().apply { id = teamId } else null,
                    space = if (spaceId != null) Space().apply { id = spaceId } else null,
                    intro = intro,
                    description = description,
                    submissionSchema = submissionSchema,
                    rank = rank,
                    approved =
                        if (spaceId != null || teamId != null) ApproveType.NONE
                        else ApproveType.APPROVED,
                    rejectReason = "",
                )
            )
        return task.id!!
    }

    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    fun updateTaskName(taskId: IdType, name: String) {
        val task = getTask(taskId)
        task.name = name
        taskRepository.save(task)
    }

    fun updateTaskDeadline(taskId: IdType, deadline: LocalDateTime?) {
        val task = getTask(taskId)
        task.deadline = deadline
        taskRepository.save(task)
    }

    fun updateTaskParticipantLimit(taskId: IdType, participantLimit: Int?) {
        val task = getTask(taskId)
        task.participantLimit = participantLimit
        taskRepository.save(task)
    }

    fun updateTaskDefaultDeadline(taskId: IdType, defaultDeadline: Long) {
        val task = getTask(taskId)
        task.defaultDeadline = defaultDeadline
        taskRepository.save(task)
    }

    fun updateTaskResubmittable(taskId: IdType, resubmittable: Boolean) {
        val task = getTask(taskId)
        task.resubmittable = resubmittable
        taskRepository.save(task)
    }

    fun updateTaskEditable(taskId: IdType, editable: Boolean) {
        val task = getTask(taskId)
        task.editable = editable
        taskRepository.save(task)
    }

    fun updateTaskIntro(taskId: IdType, intro: String) {
        val task = getTask(taskId)
        task.intro = intro
        taskRepository.save(task)
    }

    fun updateTaskDescription(taskId: IdType, description: String) {
        val task = getTask(taskId)
        task.description = description
        taskRepository.save(task)
    }

    fun updateTaskSubmissionSchema(taskId: IdType, submissionSchema: List<TaskSubmissionSchema>) {
        val task = getTask(taskId)
        task.submissionSchema = submissionSchema
        taskRepository.save(task)
    }

    fun updateTaskRank(taskId: IdType, rank: Int?) {
        val task = getTask(taskId)
        task.rank = rank
        taskRepository.save(task)
    }

    fun updateApproved(taskId: IdType, approved: ApproveType) {
        val task = getTask(taskId)
        task.approved = approved
        taskRepository.save(task)
    }

    fun updateRejectReason(taskId: IdType, rejectReason: String) {
        val task = getTask(taskId)
        task.rejectReason = rejectReason
        taskRepository.save(task)
    }

    enum class TasksSortBy {
        DEADLINE,
        CREATED_AT,
        UPDATED_AT,
    }

    fun getTaskSumbitterType(taskId: IdType): TaskSubmitterTypeDTO {
        val task = getTask(taskId)
        return convertTaskSubmitterType(task.submitterType!!)
    }

    fun getTaskSpaceId(taskId: IdType): IdType? {
        val task = getTask(taskId)
        return task.space?.id
    }

    fun getTaskTeamId(taskId: IdType): IdType? {
        val task = getTask(taskId)
        return task.team?.id
    }

    fun getTaskSubmittersSummary(taskId: IdType): TaskSubmittersDTO {
        val submitters = taskMembershipRepository.findByTaskIdWhereMemberHasSubmitted(taskId)
        val examples = submitters.sortedBy { it.updatedAt }.reversed().take(3)
        val exampleDTOs =
            when (getTaskSumbitterType(taskId)) {
                USER -> examples.map { userService.getUserAvatarId(it.memberId!!) }
                TEAM -> examples.map { teamService.getTeamAvatarId(it.memberId!!) }
            }.map { TaskSubmittersExamplesInnerDTO(it) }
        return TaskSubmittersDTO(total = submitters.size, examples = exampleDTOs)
    }

    fun enumerateTasks(
        enumerateOptions: TaskEnumerateOptions,
        keywords: String?,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        queryOptions: TaskQueryOptions,
    ): Pair<List<TaskDTO>, PageDTO> {
        if (keywords == null) {
            return enumerateTasksUseDatabase(
                enumerateOptions,
                pageSize,
                pageStart,
                sortBy,
                sortOrder,
                queryOptions,
            )
        } else {
            return enumerateTasksUseElasticSearch(
                enumerateOptions,
                keywords,
                pageSize,
                pageStart,
                sortBy,
                sortOrder,
                queryOptions,
            )
        }
    }

    /**
     * Creates a JPA Specification for filtering tasks based on various criteria.
     *
     * SQL Example:
     * ```
     * SELECT t.* FROM task t
     * WHERE t.space_id = ? AND t.team_id = ? AND t.approved = ?
     *   AND t.creator_id = ?
     *   AND EXISTS (
     *     SELECT 1 FROM task_topics_relation ttr
     *     WHERE ttr.task_id = t.id AND ttr.topic_id IN (?, ?, ?)
     *   )
     *   AND -- joined predicate (see createJoinedPredicate)
     * ```
     */
    private fun createTaskSpecification(
        options: TaskEnumerateOptions,
        currentUserId: IdType,
    ): Specification<Task> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            // Space filter
            options.space?.let {
                predicates.add(cb.equal(root.get<Space>("space").get<IdType>("id"), it))
            }

            // Team filter
            options.team?.let {
                predicates.add(cb.equal(root.get<Team>("team").get<IdType>("id"), it))
            }

            // Approved status filter
            options.approved?.let {
                predicates.add(cb.equal(root.get<ApproveType>("approved"), it))
            }

            // Owner filter
            options.owner?.let {
                predicates.add(cb.equal(root.get<User>("creator").get<IdType>("id"), it))
            }

            // Topics filter
            options.topics?.let { topics ->
                if (topics.isNotEmpty()) {
                    val subquery = query!!.subquery(TaskTopicsRelation::class.java)
                    val subroot = subquery.from(TaskTopicsRelation::class.java)
                    subquery
                        .select(subroot)
                        .where(
                            cb.equal(
                                subroot.get<Task>("task").get<IdType>("id"),
                                root.get<IdType>("id"),
                            ),
                            subroot.get<Topic>("topic").get<Int>("id").`in`(topics),
                        )
                    predicates.add(cb.exists(subquery))
                }
            }

            options.joined?.let { joined ->
                predicates.add(createJoinedPredicate(root, query!!, cb, currentUserId, joined))
            }

            // Combine all predicates
            if (predicates.isEmpty()) {
                null
            } else {
                cb.and(*predicates.toTypedArray())
            }
        }
    }

    /**
     * Creates a predicate for filtering tasks by whether the current user has joined them. Handles
     * both USER and TEAM type submitters with appropriate logic for each.
     *
     * SQL Example:
     * ```
     * -- For joined=true
     * (
     *   -- User joined directly (see createUserJoinedPredicate)
     *   OR
     *   -- User joined via team (see createTeamJoinedPredicate)
     * )
     *
     * -- For joined=false
     * NOT (
     *   -- User joined directly OR User joined via team
     * )
     * ```
     */
    private fun createJoinedPredicate(
        root: Root<Task>,
        query: CriteriaQuery<*>,
        cb: CriteriaBuilder,
        currentUserId: IdType,
        joined: Boolean,
    ): Predicate {
        if (query.isDistinct == false) {
            query.distinct(true)
        }

        // For USER type submitters, check TaskMembership directly
        val userJoinedPredicate = createUserJoinedPredicate(root, query, cb, currentUserId)

        // For TEAM type submitters, need to check if user is in a team that joined the task
        val teamJoinedPredicate = createTeamJoinedPredicate(root, query, cb, currentUserId)

        // Combine with OR for any join type
        val joinedPredicate = cb.or(userJoinedPredicate, teamJoinedPredicate)

        // Return the appropriate predicate based on the joined flag
        return if (joined) {
            joinedPredicate
        } else {
            cb.not(joinedPredicate)
        }
    }

    /**
     * Creates a predicate for USER type task submissions. Checks if the user has directly joined
     * the task.
     *
     * SQL Example:
     * ```
     * t.submitter_type = 'USER' AND EXISTS (
     *   SELECT 1 FROM task_membership tm
     *   WHERE tm.task_id = t.id AND tm.member_id = ?
     * )
     * ```
     */
    private fun createUserJoinedPredicate(
        root: Root<Task>,
        query: CriteriaQuery<*>,
        cb: CriteriaBuilder,
        userId: IdType,
    ): Predicate {
        // Check USER type and direct membership
        val isUserType =
            cb.equal(root.get<TaskSubmitterType>("submitterType"), TaskSubmitterType.USER)

        // Subquery to check if user directly joined the task
        val directMembershipSubquery = query.subquery(TaskMembership::class.java)
        val membershipRoot = directMembershipSubquery.from(TaskMembership::class.java)

        directMembershipSubquery
            .select(membershipRoot)
            .where(
                cb.equal(
                    membershipRoot.get<Task>("task").get<IdType>("id"),
                    root.get<IdType>("id"),
                ),
                cb.equal(membershipRoot.get<IdType>("memberId"), userId),
            )

        // User can only join USER type tasks directly
        return cb.and(isUserType, cb.exists(directMembershipSubquery))
    }

    /**
     * Creates a predicate for TEAM type task submissions. Checks if the user is a member of any
     * team that has joined the task.
     *
     * SQL Example:
     * ```
     * t.submitter_type = 'TEAM' AND EXISTS (
     *   SELECT 1 FROM team_user_relation tur
     *   JOIN team te ON tur.team_id = te.id
     *   WHERE tur.user_id = ?
     *   AND EXISTS (
     *     SELECT 1 FROM task_membership tm
     *     WHERE tm.task_id = t.id AND tm.member_id = te.id
     *   )
     * )
     * ```
     */
    private fun createTeamJoinedPredicate(
        root: Root<Task>,
        query: CriteriaQuery<*>,
        cb: CriteriaBuilder,
        userId: IdType,
    ): Predicate {
        // Check TEAM type
        val isTeamType =
            cb.equal(root.get<TaskSubmitterType>("submitterType"), TaskSubmitterType.TEAM)

        // Subquery to check if user joined through a team
        val teamMembershipSubquery = query.subquery(TeamUserRelation::class.java)
        val relationRoot = teamMembershipSubquery.from(TeamUserRelation::class.java)
        val teamMembershipJoin = relationRoot.join<TeamUserRelation, Team>("team", JoinType.INNER)

        // Need another subquery to check if the team joined the task
        val taskTeamSubquery = teamMembershipSubquery.subquery(TaskMembership::class.java)
        val taskMembershipRoot = taskTeamSubquery.from(TaskMembership::class.java)

        taskTeamSubquery
            .select(taskMembershipRoot)
            .where(
                cb.equal(
                    taskMembershipRoot.get<Task>("task").get<IdType>("id"),
                    root.get<IdType>("id"),
                ),
                cb.equal(
                    taskMembershipRoot.get<IdType>("memberId"),
                    teamMembershipJoin.get<IdType>("id"),
                ),
            )

        teamMembershipSubquery
            .select(relationRoot)
            .where(
                cb.equal(relationRoot.get<User>("user").get<IdType>("id"), userId),
                cb.exists(taskTeamSubquery),
            )

        // User can only join TEAM type tasks through a team
        return cb.and(isTeamType, cb.exists(teamMembershipSubquery))
    }

    fun enumerateTasksUseDatabase(
        options: TaskEnumerateOptions,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        queryOptions: TaskQueryOptions,
    ): Pair<List<TaskDTO>, PageDTO> {
        val sortProperty =
            when (sortBy) {
                TasksSortBy.CREATED_AT -> Task::createdAt
                TasksSortBy.UPDATED_AT -> Task::updatedAt
                TasksSortBy.DEADLINE -> Task::updatedAt
            }

        val direction = sortOrder.toJpaDirection()

        // Get current user ID for joined filter
        val currentUserId = authenticationService.getCurrentUserId()

        // Create a specification for filtering tasks
        val specification = createTaskSpecification(options, currentUserId)

        // Create cursor spec with sort by the requested property but using ID as cursor
        val cursorSpec =
            taskRepository
                .idSeekSpec(Task::id, sortProperty, direction)
                .specification(specification)
                .build()

        // Execute the query with cursor pagination
        val (content, pageInfo) =
            taskRepository.findAllWithIdCursor(cursorSpec, pageStart, pageSize)

        return Pair(content.map { it.toTaskDTO(queryOptions) }, pageInfo.toPageDTO())
    }

    fun enumerateTasksUseElasticSearch(
        options: TaskEnumerateOptions,
        keywords: String,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        queryOptions: TaskQueryOptions,
    ): Pair<List<TaskDTO>, PageDTO> {
        val criteria = Criteria("name").matches(keywords)
        val query = org.springframework.data.elasticsearch.core.query.CriteriaQuery(criteria)
        val hints = elasticsearchTemplate.search(query, TaskElasticSearch::class.java)
        val result =
            (SearchHitSupport.unwrapSearchHits(hints) as List<*>).filterIsInstance<
                TaskElasticSearch
            >()
        var entities = taskRepository.findAllById(result.map { it.id })
        if (options.space != null) entities = entities.filter { it.space?.id == options.space }
        if (options.team != null) entities = entities.filter { it.team?.id == options.team }
        if (options.approved != null) entities = entities.filter { it.approved == options.approved }
        if (options.owner != null)
            entities = entities.filter { it.creator?.id == options.owner.toInt() }
        if (options.joined != null)
            entities =
                entities.filter {
                    taskMembershipService
                        .getJoined(it, authenticationService.getCurrentUserId())
                        .first == options.joined
                }
        if (options.topics != null)
            entities =
                entities.filter { task ->
                    val topics = taskTopicsService.getTaskTopicIds(task.id!!)
                    options.topics.intersect(topics).isNotEmpty()
                }
        val (tasks, page) =
            PageHelper.pageFromAll(
                entities,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("task", id) },
            )
        val dtos = tasks.map { getTaskDto(it.id!!, queryOptions) }
        return Pair(dtos, page)
    }

    fun deleteTask(taskId: IdType) {
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
}
