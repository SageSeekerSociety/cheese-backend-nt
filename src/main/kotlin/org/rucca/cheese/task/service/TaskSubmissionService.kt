/*
 *  Description: This file implements the TaskSubmissionService class.
 *               It is responsible for CRUD of task submissions.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *
 */

package org.rucca.cheese.task.service

import java.time.LocalDateTime
import org.hibernate.query.SortDirection
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.attachment.AttachmentService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.pagination.model.SimpleCursor
import org.rucca.cheese.common.pagination.model.toPageDTO
import org.rucca.cheese.common.pagination.util.toJpaDirection
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.query.dsl.queryFor
import org.rucca.cheese.common.query.internal.spec.col
import org.rucca.cheese.common.query.internal.spec.div
import org.rucca.cheese.common.query.internal.spec.exists
import org.rucca.cheese.common.query.internal.spec.notExists
import org.rucca.cheese.common.query.internal.spec.parent
import org.rucca.cheese.common.query.internal.spec.subquery
import org.rucca.cheese.common.query.model.CursorMode
import org.rucca.cheese.common.query.runtime.findWithQueryObject
import org.rucca.cheese.model.*
import org.rucca.cheese.task.*
import org.rucca.cheese.task.error.TaskNotResubmittableError
import org.rucca.cheese.task.error.TaskSubmissionNotEditableError
import org.rucca.cheese.task.error.TaskSubmissionNotMatchSchemaError
import org.rucca.cheese.task.error.TaskVersionNotSubmittedYetError
import org.rucca.cheese.task.event.TaskMembershipStatusUpdateEvent
import org.rucca.cheese.user.User
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class TaskSubmissionService(
    private val userService: UserService,
    private val taskRepository: TaskRepository,
    private val taskMembershipRepository: TaskMembershipRepository,
    private val taskSubmissionRepository: TaskSubmissionRepository,
    private val attachmentService: AttachmentService,
    private val taskSubmissionEntryRepository: TaskSubmissionEntryRepository,
    private val taskSubmissionReviewService: TaskSubmissionReviewService,
    private val taskMembershipService: TaskMembershipService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(TaskSubmissionService::class.java)

    sealed class TaskSubmissionEntry {
        data class Text(val text: String) : TaskSubmissionEntry()

        data class Attachment(val attachmentId: IdType) : TaskSubmissionEntry()
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

    fun getSubmissionDTO(submissionId: IdType, queryReview: Boolean = false): TaskSubmissionDTO {
        val submission = getSubmission(submissionId)
        return submission.toTaskSubmissionDTO(queryReview = queryReview)
    }

    fun isTaskOwnerOfSubmission(submissionId: IdType, userId: IdType): Boolean {
        val submission = getSubmission(submissionId)
        return submission.membership!!.task!!.creator!!.id!!.toLong() == userId
    }

    fun taskHasAnySubmission(taskId: IdType): Boolean {
        return taskSubmissionRepository.existsByTaskId(taskId)
    }

    fun validateSubmission(taskId: IdType, submission: List<TaskSubmissionEntry>) {
        val schema =
            taskRepository
                .findById(taskId)
                .orElseThrow { NotFoundError("task", taskId) }
                .submissionSchema!!
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
        submitterId: IdType,
        version: Int,
        submissions: List<TaskSubmissionEntry>,
        schema: List<TaskSubmissionSchema>? = null,
    ): TaskSubmissionDTO {
        val submission =
            TaskSubmission(
                membership = participant,
                version = version,
                submitter = User().apply { id = submitterId.toInt() },
            )
        val savedSubmission = taskSubmissionRepository.save(submission)
        val entries =
            submissions.withIndex().map {
                val text =
                    when (val entry = submissions[it.index]) {
                        is TaskSubmissionEntry.Text -> entry.text
                        is TaskSubmissionEntry.Attachment -> null
                    }
                val attachment =
                    when (val entry = submissions[it.index]) {
                        is TaskSubmissionEntry.Text -> null
                        is TaskSubmissionEntry.Attachment ->
                            Attachment().apply { id = entry.attachmentId.toInt() }
                    }
                TaskSubmissionEntry(
                    taskSubmission = TaskSubmission().apply { id = submission.id },
                    index = it.index,
                    contentText = text,
                    contentAttachment = attachment,
                )
            }
        taskSubmissionEntryRepository.saveAll(entries)

        val membershipId = savedSubmission.membership?.id
        if (membershipId != null) {
            eventPublisher.publishEvent(TaskMembershipStatusUpdateEvent(this, membershipId))
            log.debug(
                "Published status update event after creating submission {} for membership {}",
                savedSubmission.id,
                membershipId,
            )
        } else {
            log.warn("Saved TaskSubmission {} has no associated membership ID.", savedSubmission.id)
        }

        return submission.toTaskSubmissionDTO(entries, schema)
    }

    private fun deleteTaskSubmission(participant: TaskMembership, version: Int) {
        val entries =
            taskSubmissionRepository.findAllByMembershipIdAndVersion(participant.id!!, version)
        if (entries.isEmpty()) {
            throw TaskVersionNotSubmittedYetError(
                participant.task!!.id!!,
                participant.memberId!!,
                version,
            )
        }
        for (entry in entries) {
            entry.deletedAt = LocalDateTime.now()
        }
        taskSubmissionRepository.saveAll(entries)
    }

    fun submitTask(
        taskId: IdType,
        participantId: IdType,
        submitterId: IdType,
        submission: List<TaskSubmissionEntry>,
    ): TaskSubmissionDTO {
        validateSubmission(taskId, submission)
        val participant =
            taskMembershipRepository.findById(participantId).orElseThrow {
                NotFoundError("task membership", participantId)
            }
        val oldVersion =
            taskSubmissionRepository.findVersionNumberByMembershipId(participantId).orElse(0)
        if (oldVersion > 0 && !isTaskResubmittable(taskId)) {
            throw TaskNotResubmittableError(taskId)
        }
        val newVersion = oldVersion + 1
        return createTaskSubmission(participant, submitterId, newVersion, submission)
    }

    fun modifySubmission(
        taskId: IdType,
        participantId: IdType,
        submitterId: IdType,
        version: Int,
        submission: List<TaskSubmissionEntry>,
    ): TaskSubmissionDTO {
        validateSubmission(taskId, submission)
        val participant =
            taskMembershipRepository.findById(participantId).orElseThrow {
                NotFoundError("task membership", participantId)
            }
        if (!isTaskEditable(taskId)) {
            throw TaskSubmissionNotEditableError(taskId)
        }
        deleteTaskSubmission(participant, version)
        return createTaskSubmission(participant, submitterId, version, submission)
    }

    enum class TaskSubmissionSortBy {
        CREATED_AT,
        UPDATED_AT,
    }

    fun TaskSubmission.toTaskSubmissionDTO(
        submissionList: List<org.rucca.cheese.task.TaskSubmissionEntry>? = null,
        schema: List<TaskSubmissionSchema>? = null,
        queryReview: Boolean = false,
    ): TaskSubmissionDTO {
        val submissionListNotNull =
            submissionList ?: taskSubmissionEntryRepository.findAllByTaskSubmissionId(this.id!!)
        val schemaNotNull =
            if (schema == null) {
                val membership =
                    taskMembershipRepository.findById(this.membership!!.id!!).orElseThrow {
                        RuntimeException(
                            "Invalid TaskSubmission: membership ${this.membership.id!!} does not exist"
                        )
                    }
                val task =
                    taskRepository.findById(membership!!.task!!.id!!).orElseThrow {
                        RuntimeException(
                            "Membership ${this.membership.id!!} refers to task ${membership.task!!.id} which does not exist"
                        )
                    }
                task.submissionSchema!!
            } else {
                schema
            }
        return TaskSubmissionDTO(
            id = this.id!!,
            version = this.version!!,
            createdAt = this.createdAt.toEpochMilli(),
            updatedAt = this.updatedAt.toEpochMilli(),
            member =
                taskMembershipService
                    .getTaskMembershipDTO(this.membership!!.task!!.id!!, this.membership.memberId!!)
                    .member,
            submitter = userService.getUserDto(this.submitter!!.id!!.toLong()),
            content =
                submissionListNotNull.withIndex().map {
                    TaskSubmissionContentEntryDTO(
                        title = schemaNotNull[it.index].description!!,
                        type = convertTaskSubmissionEntryType(schemaNotNull[it.index].type!!),
                        contentText = it.value.contentText,
                        contentAttachment =
                            if (it.value.contentAttachment != null)
                                attachmentService.getAttachmentDto(
                                    it.value.contentAttachment!!.id!!.toLong()
                                )
                            else null,
                    )
                },
            review = if (queryReview) taskSubmissionReviewService.getReviewDTO(this.id!!) else null,
        )
    }

    fun enumerateSubmissions(
        taskId: IdType,
        participantId: IdType?,
        allVersions: Boolean,
        queryReview: Boolean,
        reviewed: Boolean?,
        pageSize: Int,
        pageStart: IdType?,
        sortBy: TaskSubmissionSortBy,
        sortOrder: SortDirection,
    ): Pair<List<TaskSubmissionDTO>, PageDTO> {
        val sortByProperty =
            when (sortBy) {
                TaskSubmissionSortBy.CREATED_AT -> TaskSubmission::createdAt
                TaskSubmissionSortBy.UPDATED_AT -> TaskSubmission::updatedAt
            }

        val direction = sortOrder.toJpaDirection()

        val effectivePageSize = pageSize

        val queryObject =
            queryFor<TaskSubmission> {
                id(TaskSubmission::id)

                filters {
                    TaskSubmission::membership / TaskMembership::task / Task::id eq taskId

                    participantId?.let { TaskSubmission::membership / TaskMembership::id eq it }

                    whereIf(!allVersions) {
                        TaskSubmission::version eq
                            subquery {
                                selectExpr(TaskSubmission::version.max())
                                where {
                                    col(TaskSubmission::membership / TaskMembership::id) eq
                                        parent(TaskSubmission::membership / TaskMembership::id)
                                }
                            }
                    }

                    whereIf(reviewed != null) {
                        if (reviewed == true) {
                            exists {
                                col(TaskSubmissionReview::submission / TaskSubmission::id) eq
                                    parent(TaskSubmission::id)
                            }
                        } else {
                            notExists {
                                col(TaskSubmissionReview::submission / TaskSubmission::id) eq
                                    parent(TaskSubmission::id)
                            }
                        }
                    }
                }

                sort { by(sortByProperty, direction) }

                paginate {
                    cursorMode = CursorMode.ID_SEEK
                    this.pageSize = effectivePageSize
                }
            }

        val cursor = pageStart?.let { SimpleCursor.of<TaskSubmission, IdType>(it) }
        val page =
            taskSubmissionRepository.findWithQueryObject(queryObject, cursor, effectivePageSize)

        val submissions = page.content.map { it.toTaskSubmissionDTO(queryReview = queryReview) }
        val pageInfo = page.pageInfo.toPageDTO()
        return Pair(submissions, pageInfo)
    }

    private fun getTask(taskId: IdType): Task {
        return taskRepository.findById(taskId).orElseThrow { NotFoundError("task", taskId) }
    }

    private fun isTaskResubmittable(taskId: IdType): Boolean {
        val task = getTask(taskId)
        return task.resubmittable!!
    }

    private fun isTaskEditable(taskId: IdType): Boolean {
        val task = getTask(taskId)
        return task.editable!!
    }

    private fun getSubmission(submissionId: IdType): TaskSubmission {
        return taskSubmissionRepository.findById(submissionId).orElseThrow {
            NotFoundError("task submission", submissionId)
        }
    }
}

fun List<TaskSubmissionContentDTO>.toEntryList() = map {
    if (it.text != null) {
        TaskSubmissionService.TaskSubmissionEntry.Text(it.text)
    } else if (it.attachmentId != null) {
        TaskSubmissionService.TaskSubmissionEntry.Attachment(it.attachmentId)
    } else {
        throw IllegalArgumentException("Invalid TaskSubmissionContentDTO: $it")
    }
}
