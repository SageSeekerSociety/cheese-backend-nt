package org.rucca.cheese.task

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import java.time.LocalDate
import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.error.PermissionDeniedError
import org.rucca.cheese.common.config.ApplicationConfig
import org.rucca.cheese.common.error.BaseError
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.model.*
import org.rucca.cheese.model.TaskSubmitterTypeDTO.*
import org.rucca.cheese.space.Space
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.space.SpaceUserRankService
import org.rucca.cheese.task.error.*
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamService
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
    private val taskMembershipRepository: taskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val entityManager: EntityManager,
    private val elasticsearchTemplate: ElasticsearchTemplate,
    private val spaceUserRankService: SpaceUserRankService,
    private val applicationConfig: ApplicationConfig,
    private val spaceService: SpaceService,
) {
    fun getTaskDto(
        taskId: IdType,
        querySpace: Boolean = false,
        queryTeam: Boolean = false,
        queryJoinability: Boolean = false,
        querySubmittability: Boolean = false,
        queryJoined: Boolean = false,
    ): TaskDTO {
        val task = getTask(taskId)
        return task.toTaskDTO(
            querySpace,
            queryTeam,
            queryJoinability,
            querySubmittability,
            queryJoined
        )
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

    fun Task.toTaskDTO(
        querySpace: Boolean,
        queryTeam: Boolean,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        queryJoined: Boolean
    ): TaskDTO {
        val userId = authenticationService.getCurrentUserId()
        val space =
            if (querySpace && this.space!!.id != null) spaceService.getSpaceDto(this.space.id!!)
            else null
        val team =
            if (queryTeam && this.team!!.id != null) teamService.getTeamDto(this.team.id!!)
            else null
        val joinability = if (queryJoinability) getJoinability(this, userId) else Pair(null, null)
        val submittability =
            if (querySubmittability) getSubmittability(this, userId) else Pair(null, null)
        val joined = if (queryJoined) getJoined(this, userId) else Pair(null, null)
        return TaskDTO(
            id = this.id!!,
            name = this.name!!,
            submitterType = convertTaskSubmitterType(this.submitterType!!),
            creator = userService.getUserDto(this.creator!!.id!!.toLong()),
            deadline = this.deadline?.toEpochMilli(),
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
                            convertTaskSubmissionEntryType(it.type!!)
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
        )
    }

    fun createTask(
        name: String,
        submitterType: TaskSubmitterType,
        deadline: LocalDateTime?,
        defaultDeadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<TaskSubmissionSchema>,
        creatorId: IdType,
        teamId: IdType?,
        spaceId: IdType?,
        rank: Int? = null
    ): IdType {
        val task =
            taskRepository.save(
                Task(
                    name = name,
                    submitterType = submitterType,
                    creator = User().apply { id = creatorId.toInt() },
                    deadline = deadline,
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

    private fun getTaskMembership(taskId: IdType, memberId: IdType): TaskMembership {
        return taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
            NotTaskParticipantYetError(taskId, memberId)
        }
    }

    fun getTaskMembershipDTO(taskId: IdType, memberId: IdType): TaskMembershipDTO {
        val membership = getTaskMembership(taskId, memberId)
        val taskParticipantSummaryDto =
            when (getTaskSumbitterType(taskId)) {
                USER ->
                    userService.getTaskParticipantSummaryDto(
                        membership.memberId!!,
                        membership.approved!!
                    )
                TEAM ->
                    teamService.getTaskParticipantSummaryDto(
                        membership.memberId!!,
                        membership.approved!!
                    )
            }
        return TaskMembershipDTO(
            id = membership.id!!,
            member = taskParticipantSummaryDto,
            createdAt = membership.createdAt!!.toEpochMilli(),
            updatedAt = membership.updatedAt!!.toEpochMilli(),
            deadline = membership.deadline?.toEpochMilli(),
            approved = membership.approved!!.convert(),
        )
    }

    fun updateTaskMembershipDeadline(
        taskId: IdType,
        memberId: IdType,
        deadline: Long,
    ) {
        val participant = getTaskMembership(taskId, memberId)
        if (participant.approved == ApproveType.APPROVED) {
            participant.deadline = deadline.toLocalDateTime()
        } else {
            throw TaskParticipantNotApprovedError(taskId, memberId)
        }
        taskMembershipRepository.save(participant)
    }

    fun updateTaskMembershipApproved(taskId: IdType, memberId: IdType, approved: ApproveType) {
        val participant = getTaskMembership(taskId, memberId)
        participant.approved = approved
        taskMembershipRepository.save(participant)
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

    fun isTaskJoinable(task: Task, memberId: IdType): BaseError? {
        // Task is approved
        if (task.approved != ApproveType.APPROVED)
            return PermissionDeniedError("add-participant", "task", task.id, null)

        // Ensure member exists
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                if (!userService.existsUser(memberId)) return NotFoundError("user", memberId)
            TaskSubmitterType.TEAM ->
                if (!teamService.existsTeam(memberId)) return NotFoundError("team", memberId)
        }

        // Has not joined yet
        if (taskMembershipRepository.existsByTaskIdAndMemberId(task.id!!, memberId))
            return AlreadyBeTaskParticipantError(task.id!!, memberId)

        // Have enough rank
        val needToCheckRank =
            applicationConfig.rankCheckEnforced &&
                task.submitterType == TaskSubmitterType.USER &&
                task.space != null &&
                task.space.enableRank!! &&
                task.rank != null
        if (needToCheckRank) {
            val requiredRank = task.rank!! - applicationConfig.rankJump
            val acturalRank = spaceUserRankService.getRank(task.space!!.id!!, memberId)
            if (acturalRank < requiredRank)
                return YourRankIsNotHighEnoughError(acturalRank, requiredRank)
        }

        return null
    }

    fun getJoinability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER -> return Pair(isTaskJoinable(task, userId) == null, null)
            TaskSubmitterType.TEAM -> {
                val teams =
                    teamService.getTeamsThatUserCanUseToJoinTask(task.id!!, userId).filter {
                        isTaskJoinable(task, it.id) == null
                    }
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getSubmittability(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberIdAndApproved(
                        task.id!!,
                        userId,
                        ApproveType.APPROVED
                    ),
                    null
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserCanUseToSubmitTask(task.id!!, userId)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getJoined(task: Task, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        when (task.submitterType!!) {
            TaskSubmitterType.USER ->
                return Pair(
                    taskMembershipRepository.existsByTaskIdAndMemberId(
                        task.id!!,
                        userId,
                    ),
                    null
                )
            TaskSubmitterType.TEAM -> {
                val teams = teamService.getTeamsThatUserJoinedTaskAs(task.id!!, userId)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
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
        space: IdType?,
        team: IdType?,
        approved: ApproveType?,
        owner: IdType?,
        joined: Boolean?,
        keywords: String?,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        querySpace: Boolean,
        queryTeam: Boolean,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        queryJoined: Boolean,
    ): Pair<List<TaskDTO>, PageDTO> {
        if (keywords == null) {
            return enumerateTasksUseDatabase(
                space,
                team,
                approved,
                owner,
                joined,
                pageSize,
                pageStart,
                sortBy,
                sortOrder,
                querySpace,
                queryTeam,
                queryJoinability,
                querySubmittability,
                queryJoined,
            )
        } else {
            val id = keywords.toLongOrNull()
            if (id != null) {
                return Pair(
                    listOf(getTaskDto(id, queryJoinability, querySubmittability)),
                    PageDTO(
                        pageStart = id,
                        pageSize = 1,
                        hasPrev = false,
                        hasMore = false,
                    )
                )
            }
            return enumerateTasksUseElasticSearch(
                space,
                team,
                approved,
                owner,
                joined,
                keywords,
                pageSize,
                pageStart,
                sortBy,
                sortOrder,
                querySpace,
                queryTeam,
                queryJoinability,
                querySubmittability,
                queryJoined,
            )
        }
    }

    fun enumerateTasksUseDatabase(
        space: IdType?,
        team: IdType?,
        approved: ApproveType?,
        owner: IdType?,
        joined: Boolean?,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        querySpace: Boolean,
        queryTeam: Boolean,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        queryJoined: Boolean,
    ): Pair<List<TaskDTO>, PageDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Task::class.java)
        val root = cq.from(Task::class.java)
        val predicates = mutableListOf<Predicate>()
        if (space != null) {
            predicates.add(cb.equal(root.get<Space>("space").get<IdType>("id"), space))
        }
        if (team != null) {
            predicates.add(cb.equal(root.get<Team>("team").get<IdType>("id"), team))
        }
        if (approved != null) {
            predicates.add(cb.equal(root.get<ApproveType>("approved"), approved))
        }
        if (owner != null) {
            predicates.add(cb.equal(root.get<User>("creator").get<IdType>("id"), owner))
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
        if (joined != null)
            result = result.filter { getJoined(it, authenticationService.getCurrentUserId()).first }
        val (curr, page) =
            PageHelper.pageFromAll(
                result,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("task", id) }
            )
        return Pair(
            curr.map {
                it.toTaskDTO(
                    querySpace = querySpace,
                    queryTeam = queryTeam,
                    queryJoinability = queryJoinability,
                    querySubmittability = querySubmittability,
                    queryJoined = queryJoined,
                )
            },
            page
        )
    }

    fun enumerateTasksUseElasticSearch(
        space: IdType?,
        team: IdType?,
        approved: ApproveType?,
        owner: IdType?,
        joined: Boolean?,
        keywords: String,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TasksSortBy,
        sortOrder: SortDirection,
        querySpace: Boolean,
        queryTeam: Boolean,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        queryJoined: Boolean,
    ): Pair<List<TaskDTO>, PageDTO> {
        val criteria = Criteria("name").matches(keywords)
        val query = CriteriaQuery(criteria)
        val hints = elasticsearchTemplate.search(query, TaskElasticSearch::class.java)
        val result =
            (SearchHitSupport.unwrapSearchHits(hints) as List<*>).filterIsInstance<
                TaskElasticSearch
            >()
        var entities = taskRepository.findAllById(result.map { it.id })
        if (querySpace) entities = entities.filter { it.space?.id == space }
        if (queryTeam) entities = entities.filter { it.team?.id == team }
        if (approved != null) entities = entities.filter { it.approved == approved }
        if (owner != null) entities = entities.filter { it.creator?.id == owner.toInt() }
        if (joined != null)
            entities =
                entities.filter { getJoined(it, authenticationService.getCurrentUserId()).first }
        val (tasks, page) =
            PageHelper.pageFromAll(
                entities,
                pageStart,
                pageSize,
                { it.id!! },
                { id -> throw NotFoundError("task", id) }
            )
        val dtos =
            tasks.map {
                getTaskDto(
                    it.id!!,
                    querySpace = querySpace,
                    queryTeam = queryTeam,
                    queryJoinability = queryJoinability,
                    querySubmittability = querySubmittability,
                    queryJoined = queryJoined,
                )
            }
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

    fun getTaskParticipantDtos(
        taskId: IdType,
        approveType: ApproveType?
    ): List<TaskParticipantSummaryDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(TaskMembership::class.java)
        val root = cq.from(TaskMembership::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<Task>("task").get<IdType>("id"), taskId))
        if (approveType != null) {
            predicates.add(cb.equal(root.get<ApproveType>("approved"), approveType))
        }
        cq.where(*predicates.toTypedArray())
        val query = entityManager.createQuery(cq)
        val participants = query.resultList
        // val participants = taskMembershipRepository.findAllByTaskId(taskId)
        return when (getTaskSumbitterType(taskId)) {
            USER ->
                participants.map {
                    userService.getTaskParticipantSummaryDto(it.memberId!!, it.approved!!)
                }
            TEAM ->
                participants.map {
                    teamService.getTaskParticipantSummaryDto(it.memberId!!, it.approved!!)
                }
        }
    }

    fun addTaskParticipant(
        taskId: IdType,
        memberId: IdType,
        deadline: LocalDateTime?,
        approved: ApproveType
    ) {
        val errorOpt = isTaskJoinable(getTask(taskId), memberId)
        if (errorOpt != null) throw errorOpt
        taskMembershipRepository.save(
            TaskMembership(
                task = Task().apply { id = taskId },
                memberId = memberId,
                deadline = deadline,
                approved = approved,
            )
        )
    }

    fun removeTaskParticipant(taskId: IdType, memberId: IdType) {
        val participant =
            taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                NotTaskParticipantYetError(taskId, memberId)
            }
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
    }
}
