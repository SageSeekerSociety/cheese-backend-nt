package org.rucca.cheese.task

import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.attachment.AttachmentService
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.PageHelper
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.Space
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
        private val taskSubmissionRepository: taskSubmissionRepository,
        private val entityManager: EntityManager,
        private val attachmentService: AttachmentService,
        private val elasticsearchTemplate: ElasticsearchTemplate,
) {
    fun getJoinability(taskId: IdType, userId: IdType): Pair<Boolean, List<TeamSummaryDTO>?> {
        val queryByUser = authenticationService.getCurrentUserId()
        when (getTaskSumbitterType(taskId)) {
            TaskSubmitterTypeDTO.USER ->
                    return Pair(!taskMembershipRepository.existsByTaskIdAndMemberId(taskId, queryByUser), null)
            TaskSubmitterTypeDTO.TEAM -> {
                val teams = teamService.getTeamsThatUserCanUseToJoinTask(taskId, queryByUser)
                return Pair(teams.isNotEmpty(), teams)
            }
        }
    }

    fun getTaskDto(taskId: IdType, queryJoinability: Boolean = false): TaskDTO {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        val joinability =
                if (queryJoinability) getJoinability(taskId, authenticationService.getCurrentUserId())
                else Pair(null, null)
        return TaskDTO(
                task.id!!,
                task.name!!,
                convertTaskSubmitterType(task.submitterType!!),
                userService.getUserDto(task.creator!!.id!!.toLong()),
                task.deadline!!.toEpochMilli(),
                task.resubmittable!!,
                task.editable!!,
                task.description!!,
                task.submissionSchema!!
                        .sortedBy { it.index }
                        .map {
                            TaskSubmissionSchemaEntryDTO(it.description!!, convertTaskSubmissionEntryType(it.type!!))
                        },
                getTaskSubmitterSummary(taskId),
                updatedAt = task.updatedAt!!.toEpochMilli(),
                createdAt = task.createdAt!!.toEpochMilli(),
                joinable = joinability.first,
                joinableAsTeam = joinability.second)
    }

    fun getTaskOwner(taskId: IdType): IdType {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return task.creator!!.id!!.toLong()
    }

    fun isTaskParticipant(taskId: IdType, userId: IdType, memberId: IdType): Boolean {
        return when (getTaskSumbitterType(taskId)) {
            TaskSubmitterTypeDTO.USER ->
                    userId == memberId && taskMembershipRepository.existsByTaskIdAndMemberId(taskId, memberId)
            TaskSubmitterTypeDTO.TEAM ->
                    teamService.isTeamMember(memberId, userId) &&
                            taskMembershipRepository.existsByTaskIdAndMemberId(taskId, memberId)
        }
    }

    fun participantIsSelf(taskId: IdType, userId: IdType, memberId: IdType): Boolean {
        return when (getTaskSumbitterType(taskId)) {
            TaskSubmitterTypeDTO.USER -> userId == memberId
            TaskSubmitterTypeDTO.TEAM -> teamService.isTeamMember(memberId, userId)
        }
    }

    fun convertTaskSubmitterType(type: TaskSubmitterType): TaskSubmitterTypeDTO {
        return when (type) {
            TaskSubmitterType.USER -> TaskSubmitterTypeDTO.USER
            TaskSubmitterType.TEAM -> TaskSubmitterTypeDTO.TEAM
        }
    }

    fun convertTaskSubmitterType(type: TaskSubmitterTypeDTO): TaskSubmitterType {
        return when (type) {
            TaskSubmitterTypeDTO.USER -> TaskSubmitterType.USER
            TaskSubmitterTypeDTO.TEAM -> TaskSubmitterType.TEAM
        }
    }

    fun convertTaskSubmissionEntryType(type: TaskSubmissionEntryType): TaskSubmissionTypeDTO {
        return when (type) {
            TaskSubmissionEntryType.TEXT -> TaskSubmissionTypeDTO.TEXT
            TaskSubmissionEntryType.ATTACHMENT -> TaskSubmissionTypeDTO.FILE
        }
    }

    fun convertTaskSubmissionEntryType(type: TaskSubmissionTypeDTO): TaskSubmissionEntryType {
        return when (type) {
            TaskSubmissionTypeDTO.TEXT -> TaskSubmissionEntryType.TEXT
            TaskSubmissionTypeDTO.FILE -> TaskSubmissionEntryType.ATTACHMENT
        }
    }

    fun createTask(
            name: String,
            submitterType: TaskSubmitterType,
            deadline: LocalDateTime,
            resubmittable: Boolean,
            editable: Boolean,
            description: String,
            submissionSchema: List<TaskSubmissionSchema>,
            creatorId: IdType,
            teamId: IdType? = null,
            spaceId: IdType? = null,
    ): IdType {
        val task =
                taskRepository.save(
                        Task(
                                name = name,
                                submitterType = submitterType,
                                creator = User().apply { id = creatorId.toInt() },
                                deadline = deadline,
                                resubmittable = resubmittable,
                                editable = editable,
                                team = if (teamId != null) Team().apply { id = teamId } else null,
                                space = if (spaceId != null) Space().apply { id = spaceId } else null,
                                description = description,
                                submissionSchema = submissionSchema))
        return task.id!!
    }

    fun updateTaskName(taskId: IdType, name: String) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.name = name
        taskRepository.save(task)
    }

    fun updateTaskDeadline(taskId: IdType, deadline: LocalDateTime) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.deadline = deadline
        taskRepository.save(task)
    }

    fun updateTaskResubmittable(taskId: IdType, resubmittable: Boolean) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.resubmittable = resubmittable
        taskRepository.save(task)
    }

    fun updateTaskEditable(taskId: IdType, editable: Boolean) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.editable = editable
        taskRepository.save(task)
    }

    fun updateTaskDescription(taskId: IdType, description: String) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.description = description
        taskRepository.save(task)
    }

    fun updateTaskSubmissionSchema(taskId: IdType, submissionSchema: List<TaskSubmissionSchema>) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        task.submissionSchema = submissionSchema
        taskRepository.save(task)
    }

    enum class TasksSortBy {
        DEADLINE,
        CREATED_AT,
        UPDATED_AT,
    }

    fun getTaskSumbitterType(taskId: IdType): TaskSubmitterTypeDTO {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return convertTaskSubmitterType(task.submitterType!!)
    }

    fun isTaskResubmittable(taskId: IdType): Boolean {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return task.resubmittable!!
    }

    fun isTaskEditable(taskId: IdType): Boolean {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
        return task.editable!!
    }

    fun getTaskSubmitterSummary(taskId: IdType): TaskSubmittersDTO {
        val submitters = taskMembershipRepository.findByTaskIdWhereMemberHasSubmitted(taskId)
        val examples = submitters.sortedBy { it.updatedAt }.reversed().take(3)
        val exampleDTOs =
                when (getTaskSumbitterType(taskId)) {
                    TaskSubmitterTypeDTO.USER -> examples.map { userService.getUserAvatarId(it.memberId!!) }
                    TaskSubmitterTypeDTO.TEAM -> examples.map { teamService.getTeamAvatarId(it.memberId!!) }
                }.map { TaskSubmittersExamplesInnerDTO(it) }
        return TaskSubmittersDTO(total = submitters.size, examples = exampleDTOs)
    }

    fun enumerateTasks(
            space: IdType?,
            team: Int?,
            keywords: String?,
            pageSize: Int,
            pageStart: IdType?,
            sortBy: TasksSortBy,
            sortOrder: SortDirection,
            queryJoinability: Boolean = false,
    ): Pair<List<TaskDTO>, PageDTO> {
        if (keywords == null) {
            return enumerateTasksUseDatabase(space, team, pageSize, pageStart, sortBy, sortOrder, queryJoinability)
        } else {
            val id = keywords.toLongOrNull()
            if (id != null) {
                return Pair(
                        listOf(getTaskDto(id, queryJoinability)),
                        PageDTO(
                                pageStart = id,
                                pageSize = 1,
                                hasPrev = false,
                                hasMore = false,
                        ))
            }
            return enumerateTasksUseElasticSearch(keywords, pageSize, pageStart, queryJoinability)
        }
    }

    fun enumerateTasksUseDatabase(
            space: IdType?,
            team: Int?,
            pageSize: Int,
            pageStart: IdType?,
            sortBy: TasksSortBy,
            sortOrder: SortDirection,
            queryJoinability: Boolean = false,
    ): Pair<List<TaskDTO>, PageDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(Task::class.java)
        val root = cq.from(Task::class.java)
        if (space != null) {
            cq.where(cb.equal(root.get<Space>("space").get<IdType>("id"), space))
        }
        if (team != null) {
            cq.where(cb.equal(root.get<Team>("team").get<IdType>("id"), team))
        }
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
        val result = query.resultList
        val (curr, page) =
                PageHelper.pageFromAll(
                        result, pageStart, pageSize, { it.id!! }, { id -> throw NotFoundError("task", id) })
        return Pair(
                curr.map {
                    val joinability =
                            if (queryJoinability) getJoinability(it.id!!, authenticationService.getCurrentUserId())
                            else Pair(null, null)
                    TaskDTO(
                            it.id!!,
                            it.name!!,
                            convertTaskSubmitterType(it.submitterType!!),
                            userService.getUserDto(it.creator!!.id!!.toLong()),
                            it.deadline!!.toEpochMilli(),
                            it.resubmittable!!,
                            it.editable!!,
                            it.description!!,
                            it.submissionSchema!!
                                    .sortedBy { it.index }
                                    .map {
                                        TaskSubmissionSchemaEntryDTO(
                                                it.description!!, convertTaskSubmissionEntryType(it.type!!))
                                    },
                            getTaskSubmitterSummary(it.id!!),
                            updatedAt = it.updatedAt!!.toEpochMilli(),
                            createdAt = it.createdAt!!.toEpochMilli(),
                            joinable = joinability.first,
                            joinableAsTeam = joinability.second)
                },
                page)
    }

    fun enumerateTasksUseElasticSearch(
            keywords: String,
            pageSize: Int,
            pageStart: IdType?,
            queryJoinability: Boolean = false,
    ): Pair<List<TaskDTO>, PageDTO> {
        val criteria = Criteria("name").matches(keywords)
        val query = CriteriaQuery(criteria)
        val hints = elasticsearchTemplate.search(query, TaskElasticSearch::class.java)
        val result = (SearchHitSupport.unwrapSearchHits(hints) as List<*>).filterIsInstance<TaskElasticSearch>()
        val (tasks, page) =
                PageHelper.pageFromAll(
                        result, pageStart, pageSize, { it.id!! }, { id -> throw NotFoundError("task", id) })
        return Pair(tasks.map { getTaskDto(it.id!!, queryJoinability) }, page)
    }

    fun deleteTask(taskId: IdType) {
        val task = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
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

    fun getTaskParticipantDtos(taskId: IdType): List<TaskParticipantSummaryDTO> {
        val participants = taskMembershipRepository.findAllByTaskId(taskId)
        return when (getTaskSumbitterType(taskId)) {
            TaskSubmitterTypeDTO.USER -> participants.map { userService.getTaskParticipantSummaryDto(it.memberId!!) }
            TaskSubmitterTypeDTO.TEAM -> participants.map { teamService.getTaskParticipantSummaryDto(it.memberId!!) }
        }
    }

    fun addTaskParticipant(taskId: IdType, memberId: IdType) {
        when (getTaskSumbitterType(taskId)) {
            TaskSubmitterTypeDTO.USER -> userService.ensureUserExists(memberId)
            TaskSubmitterTypeDTO.TEAM -> teamService.ensureTeamExists(memberId)
        }
        taskMembershipRepository.save(TaskMembership(task = Task().apply { id = taskId }, memberId = memberId))
    }

    fun removeTaskParticipant(taskId: IdType, memberId: IdType) {
        val participant =
                taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                    NotTaskParticipantYetError(taskId, memberId)
                }
        participant.deletedAt = LocalDateTime.now()
        taskMembershipRepository.save(participant)
    }

    sealed class TaskSubmissionEntry {
        data class Text(val text: String) : TaskSubmissionEntry()

        data class Attachment(val attachmentId: IdType) : TaskSubmissionEntry()
    }

    fun validateSubmission(taskId: IdType, submission: List<TaskSubmissionEntry>) {
        val schema = taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }.submissionSchema!!
        if (schema.size != submission.size) {
            throw TaskSubmissionNotMatchSchemaError()
        }
        for (schemaEntry in submission.withIndex()) {
            val entry = submission[schemaEntry.index]
            when (schema[schemaEntry.index].type!!) {
                TaskSubmissionEntryType.TEXT -> {
                    if (entry !is TaskSubmissionEntry.Text) {
                        throw TaskSubmissionNotMatchSchemaError()
                    }
                }
                TaskSubmissionEntryType.ATTACHMENT -> {
                    if (entry !is TaskSubmissionEntry.Attachment) {
                        throw TaskSubmissionNotMatchSchemaError()
                    }
                }
            }
        }
    }

    private fun createTaskSubmission(
            participant: TaskMembership,
            version: Int,
            submission: List<TaskSubmissionEntry>
    ): List<TaskSubmissionInnerDTO> {
        val entriesUnsaved =
                submission.withIndex().map {
                    val text =
                            when (val entry = submission[it.index]) {
                                is TaskSubmissionEntry.Text -> entry.text
                                is TaskSubmissionEntry.Attachment -> null
                            }
                    val attachment =
                            when (val entry = submission[it.index]) {
                                is TaskSubmissionEntry.Text -> null
                                is TaskSubmissionEntry.Attachment ->
                                        Attachment().apply { id = entry.attachmentId.toInt() }
                            }
                    TaskSubmission(
                            membership = TaskMembership().apply { id = participant.id },
                            version = version,
                            index = it.index,
                            contentText = text,
                            contentAttachment = attachment,
                    )
                }
        val entries = taskSubmissionRepository.saveAll(entriesUnsaved)
        return entries.map {
            TaskSubmissionInnerDTO(
                    it.id!!,
                    participant.memberId!!,
                    it.version!!,
                    it.index!!,
                    it.createdAt!!.toEpochMilli(),
                    it.updatedAt!!.toEpochMilli(),
                    it.contentText,
                    if (it.contentAttachment != null)
                            attachmentService.getAttachmentDto(it.contentAttachment.id!!.toLong())
                    else null)
        }
    }

    private fun deleteTaskSubmission(
            participant: TaskMembership,
            version: Int,
    ) {
        val entries = taskSubmissionRepository.findAllByMembershipIdAndVersion(participant.id!!, version)
        if (entries.isEmpty()) {
            throw TaskVersionNotSubmittedYetError(participant.task!!.id!!, participant.memberId!!, version)
        }
        for (entry in entries) {
            entry.deletedAt = LocalDateTime.now()
        }
        taskSubmissionRepository.saveAll(entries)
    }

    fun submitTask(
            taskId: IdType,
            memberId: IdType,
            submission: List<TaskSubmissionEntry>
    ): List<TaskSubmissionInnerDTO> {
        validateSubmission(taskId, submission)
        val participant =
                taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                    NotTaskParticipantYetError(taskId, memberId)
                }
        val oldVersion = taskSubmissionRepository.findVersionNumberByMembershipId(participant.id!!).orElse(0)
        if (oldVersion > 0 && !isTaskResubmittable(taskId)) {
            throw TaskNotResubmittableError(taskId)
        }
        val newVersion = oldVersion + 1
        return createTaskSubmission(participant, newVersion, submission)
    }

    fun modifySubmission(
            taskId: IdType,
            memberId: IdType,
            version: Int,
            submission: List<TaskSubmissionEntry>
    ): List<TaskSubmissionInnerDTO> {
        validateSubmission(taskId, submission)
        val participant =
                taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                    NotTaskParticipantYetError(taskId, memberId)
                }
        if (!isTaskEditable(taskId)) {
            throw TaskSubmissionNotEditableError(taskId)
        }
        deleteTaskSubmission(participant, version)
        return createTaskSubmission(participant, version, submission)
    }

    enum class TaskSubmissionSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    private fun mergeEntries(entries: List<TaskSubmissionInnerDTO>): List<List<TaskSubmissionInnerDTO>> {
        val result = mutableListOf<List<TaskSubmissionInnerDTO>>()
        var currentSubList = mutableListOf(entries[0])
        for (i in 1 until entries.size) {
            if (entries[i].memberId == entries[i - 1].memberId && entries[i].version == entries[i - 1].version) {
                currentSubList.add(entries[i])
            } else {
                result.add(currentSubList.sortedBy { it.index })
                currentSubList = mutableListOf(entries[i])
            }
        }
        return result
    }

    fun enumerateSubmissions(
            taskId: IdType,
            member: IdType?,
            allVersions: Boolean,
            pageSize: Int,
            pageStart: IdType?,
            sortBy: TaskSubmissionSortBy,
            sortOrder: SortDirection,
    ): Pair<List<List<TaskSubmissionInnerDTO>>, PageDTO> {
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(TaskSubmission::class.java)
        val root = cq.from(TaskSubmission::class.java)
        root.join<TaskSubmission, TaskMembership>("membership")
        cq.where(cb.equal(root.get<TaskMembership>("membership").get<IdType>("task").get<IdType>("id"), taskId))
        if (member != null) {
            cq.where(cb.equal(root.get<TaskMembership>("membership").get<IdType>("memberId"), member))
        }
        if (!allVersions) {
            val subquery = cq.subquery(Int::class.java)
            val subRoot = subquery.from(TaskSubmission::class.java)
            subquery
                    .select(cb.max(subRoot.get<Int>("version")))
                    .where(cb.equal(subRoot.get<TaskMembership>("membership"), root.get<TaskMembership>("membership")))
            cq.where(cb.equal(root.get<Int>("version"), subquery))
        }
        val by =
                when (sortBy) {
                    TaskSubmissionSortBy.CREATED_AT -> root.get<LocalDateTime>("createdAt")
                    TaskSubmissionSortBy.UPDATED_AT -> root.get<LocalDateTime>("updatedAt")
                }
        val order =
                when (sortOrder) {
                    SortDirection.ASCENDING -> cb.asc(by)
                    SortDirection.DESCENDING -> cb.desc(by)
                }
        cq.orderBy(order)
        val query = entityManager.createQuery(cq)
        val result = query.resultList
        val (curr, page) =
                PageHelper.pageFromAll(
                        result, pageStart, pageSize, { it.id!! }, { id -> throw NotFoundError("task submission", id) })
        return Pair(
                mergeEntries(
                        curr.map {
                            TaskSubmissionInnerDTO(
                                    it.id!!,
                                    it.membership!!.memberId!!,
                                    it.version!!,
                                    it.index!!,
                                    it.createdAt!!.toEpochMilli(),
                                    it.updatedAt!!.toEpochMilli(),
                                    it.contentText,
                                    if (it.contentAttachment != null)
                                            attachmentService.getAttachmentDto(it.contentAttachment.id!!.toLong())
                                    else null)
                        }),
                page)
    }
}
