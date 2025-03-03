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

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import java.time.LocalDate
import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.*
import org.rucca.cheese.model.TaskSubmitterTypeDTO.*
import org.rucca.cheese.space.Space
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.task.error.*
import org.rucca.cheese.task.option.TaskEnumerateOptions
import org.rucca.cheese.task.option.TaskQueryOptions
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamService
import org.rucca.cheese.topic.Topic
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Service

@Service
class TaskService(
    private val userService: UserService,
    private val teamService: TeamService,
    private val authenticationService: AuthenticationService,
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val entityManager: EntityManager,
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

    fun enumerateTasksUseDatabase(
        options: TaskEnumerateOptions,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        queryOptions: TaskQueryOptions,
    ): Pair<List<TaskDTO>, PageDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Task::class.java)
        val root = cq.from(Task::class.java)
        val predicates = mutableListOf<Predicate>()
        if (options.space != null) {
            predicates.add(cb.equal(root.get<Space>("space").get<IdType>("id"), options.space))
        }
        if (options.team != null) {
            predicates.add(cb.equal(root.get<Team>("team").get<IdType>("id"), options.team))
        }
        if (options.approved != null) {
            predicates.add(cb.equal(root.get<ApproveType>("approved"), options.approved))
        }
        if (options.owner != null) {
            predicates.add(cb.equal(root.get<User>("creator").get<IdType>("id"), options.owner))
        }
        if (options.topics != null) {
            val subquery = cq.subquery(TaskTopicsRelation::class.java)
            val subroot = subquery.from(TaskTopicsRelation::class.java)
            subquery
                .select(subroot)
                .where(
                    cb.equal(subroot.get<Task>("task").get<IdType>("id"), root.get<IdType>("id")),
                    subroot.get<Topic>("topic").get<Int>("id").`in`(options.topics),
                )
            predicates.add(cb.exists(subquery))
        }
        cq.where(*predicates.toTypedArray())
        val by =
            when (sortBy) {
                TasksSortBy.CREATED_AT -> root.get<LocalDateTime>("createdAt")
                TasksSortBy.UPDATED_AT -> root.get<LocalDateTime>("updatedAt")
                TasksSortBy.DEADLINE -> root.get<LocalDate>("deadline")
            }
        val order =
            when (sortOrder) {
                SortDirection.ASCENDING -> cb.asc(by)
                SortDirection.DESCENDING -> cb.desc(by)
            }
        cq.orderBy(order)
        val query = entityManager.createQuery(cq)
        var result = query.resultList
        if (options.joined != null)
            result =
                result.filter {
                    taskMembershipService
                        .getJoined(it, authenticationService.getCurrentUserId())
                        .first == options.joined
                }
        val (curr, page) =
            PageHelper.pageFromAll(
                result,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("task", id) },
            )
        return Pair(curr.map { it.toTaskDTO(queryOptions) }, page)
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
        val query = CriteriaQuery(criteria)
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
