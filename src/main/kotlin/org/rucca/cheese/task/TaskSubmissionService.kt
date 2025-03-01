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

package org.rucca.cheese.task

import org.hibernate.query.SortDirection
import org.rucca.cheese.attachment.Attachment
import org.rucca.cheese.attachment.AttachmentService
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.repository.cursorSpec
import org.rucca.cheese.common.repository.toJpaDirection
import org.rucca.cheese.common.repository.toPageDTO
import org.rucca.cheese.model.PageDTO
import org.rucca.cheese.model.TaskSubmissionContentEntryDTO
import org.rucca.cheese.model.TaskSubmissionDTO
import org.rucca.cheese.model.TaskSubmissionTypeDTO
import org.rucca.cheese.task.error.*
import org.rucca.cheese.user.User
import org.rucca.cheese.user.UserService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
) {
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
        taskSubmissionRepository.save(submission)
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
        memberId: IdType,
        submitterId: IdType,
        submission: List<TaskSubmissionEntry>,
    ): TaskSubmissionDTO {
        validateSubmission(taskId, submission)
        val participant =
            taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                NotTaskParticipantYetError(taskId, memberId)
            }
        val oldVersion =
            taskSubmissionRepository.findVersionNumberByMembershipId(participant.id!!).orElse(0)
        if (oldVersion > 0 && !isTaskResubmittable(taskId)) {
            throw TaskNotResubmittableError(taskId)
        }
        val newVersion = oldVersion + 1
        return createTaskSubmission(participant, submitterId, newVersion, submission)
    }

    fun modifySubmission(
        taskId: IdType,
        memberId: IdType,
        submitterId: IdType,
        version: Int,
        submission: List<TaskSubmissionEntry>,
    ): TaskSubmissionDTO {
        validateSubmission(taskId, submission)
        val participant =
            taskMembershipRepository.findByTaskIdAndMemberId(taskId, memberId).orElseThrow {
                NotTaskParticipantYetError(taskId, memberId)
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
                            "Membership ${this.membership.id!!} refers to task ${membership.task.id} which does not exist"
                        )
                    }
                task.submissionSchema!!
            } else {
                schema
            }
        return TaskSubmissionDTO(
            id = this.id!!,
            version = this.version!!,
            createdAt = this.createdAt!!.toEpochMilli(),
            updatedAt = this.updatedAt!!.toEpochMilli(),
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
        member: IdType?,
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

        val cursorSpec =
            taskSubmissionRepository
                .cursorSpec(TaskSubmission::id)
                .sortBy(sortByProperty, direction)
                .specification { root, query, cb ->
                    createSubmissionSpecification(
                        root,
                        query,
                        cb,
                        taskId,
                        member,
                        allVersions,
                        reviewed,
                    )
                }
                .build()

        // Execute query with cursor pagination
        val cursorPage = taskSubmissionRepository.findAllWithCursor(cursorSpec, pageStart, pageSize)

        // Convert results to DTOs and return
        val submissions =
            cursorPage.content.map { it.toTaskSubmissionDTO(queryReview = queryReview) }
        val pageInfo = cursorPage.pageInfo.toPageDTO()
        return Pair(submissions, pageInfo)
    }

    /**
     * Creates a JPA Specification for task submission queries with flexible filtering options.
     *
     * Builds predicates for filtering submissions by task, member, version, and review status. Uses
     * subqueries for complex filtering conditions like "latest version only" and "reviewed status".
     */
    private fun createSubmissionSpecification(
        root: jakarta.persistence.criteria.Root<TaskSubmission>,
        query: jakarta.persistence.criteria.CriteriaQuery<*>?,
        cb: jakarta.persistence.criteria.CriteriaBuilder,
        taskId: IdType,
        member: IdType?,
        allVersions: Boolean,
        reviewed: Boolean?,
    ): jakarta.persistence.criteria.Predicate {
        val predicates: MutableList<jakarta.persistence.criteria.Predicate> = mutableListOf()

        // Add base predicates
        predicates.add(
            cb.equal(
                root.get<TaskMembership>("membership").get<IdType>("task").get<IdType>("id"),
                taskId,
            )
        )

        if (member != null) {
            predicates.add(
                cb.equal(root.get<TaskMembership>("membership").get<IdType>("memberId"), member)
            )
        }

        // Handle version filtering
        if (!allVersions) {
            val subquery = query!!.subquery(Int::class.java)
            val subRoot = subquery.from(TaskSubmission::class.java)
            subquery
                .select(cb.max(subRoot.get<Int>("version")))
                .where(
                    cb.equal(
                        subRoot.get<TaskMembership>("membership"),
                        root.get<TaskMembership>("membership"),
                    )
                )
            predicates.add(cb.equal(root.get<Int>("version"), subquery))
        }

        // Handle review filtering
        if (reviewed != null) {
            val subquery = query!!.subquery(Boolean::class.java)
            val subRoot = subquery.from(TaskSubmissionReview::class.java)
            subquery
                .select(cb.literal(true))
                .where(cb.equal(subRoot.get<TaskSubmission>("submission"), root))
            if (reviewed) predicates.add(cb.exists(subquery))
            else predicates.add(cb.not(cb.exists(subquery)))
        }

        return cb.and(*predicates.toTypedArray())
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
