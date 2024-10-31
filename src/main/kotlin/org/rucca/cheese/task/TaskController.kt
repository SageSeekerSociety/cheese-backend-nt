package org.rucca.cheese.task

import javax.annotation.PostConstruct
import org.hibernate.query.SortDirection
import org.rucca.cheese.api.TasksApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.AuthInfo
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.annotation.ResourceId
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.space.SpaceService
import org.rucca.cheese.team.TeamService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

fun List<PostTaskSubmissionRequestInnerDTO>.toEntryList() = map {
    if (it.contentText != null) {
        TaskSubmissionService.TaskSubmissionEntry.Text(it.contentText)
    } else if (it.contentAttachmentId != null) {
        TaskSubmissionService.TaskSubmissionEntry.Attachment(it.contentAttachmentId)
    } else {
        throw IllegalArgumentException("Invalid PostTaskSubmissionRequestInnerDTO: $it")
    }
}

@RestController
class TaskController(
    private val taskService: TaskService,
    private val taskSubmissionService: TaskSubmissionService,
    private val taskSubmissionReviewService: TaskSubmissionReviewService,
    private val authorizationService: AuthorizationService,
    private val authenticationService: AuthenticationService,
    private val spaceService: SpaceService,
    private val teamService: TeamService,
) : TasksApi {
    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register("task", taskService::getTaskOwner)
        authorizationService.customAuthLogics.register("is-task-participant") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val memberId = authInfo["member"] as? IdType
            if (resourceId == null || memberId == null) {
                false
            } else {
                taskService.isTaskParticipant(resourceId, userId, memberId)
            }
        }
        authorizationService.customAuthLogics.register("is-team-task") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            if (resourceId == null) {
                false
            } else {
                taskService.getTaskSumbitterType(resourceId) == TaskSubmitterTypeDTO.TEAM
            }
        }
        authorizationService.customAuthLogics.register("is-user-task") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            if (resourceId == null) {
                false
            } else {
                taskService.getTaskSumbitterType(resourceId) == TaskSubmitterTypeDTO.USER
            }
        }
        authorizationService.customAuthLogics.register("task-member-is-self") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val memberId = authInfo["member"] as? IdType
            if (memberId == null) {
                false
            } else {
                userId == memberId
            }
        }
        authorizationService.customAuthLogics.register("task-user-is-admin-of-member") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val memberId = authInfo["member"] as? IdType
            if (memberId == null) {
                false
            } else {
                teamService.isTeamAdmin(memberId, userId)
            }
        }
        authorizationService.customAuthLogics.register("deadline-is-set") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val deadline = authInfo["deadline"] as? Long
            deadline != null
        }
        authorizationService.customAuthLogics.register("is-task-owner-of-submission") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val submissionId = authInfo["submission"] as? IdType
            if (submissionId == null) {
                false
            } else {
                taskSubmissionService.isTaskOwnerOfSubmission(submissionId, userId)
            }
        }
        authorizationService.customAuthLogics.register("is-task-approved") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val approvedQuery = authInfo["approved"] as? Boolean ?: false
            val approvedOfInstance =
                if (resourceId != null) taskService.isTaskApproved(resourceId) else false
            approvedQuery || approvedOfInstance
        }
        authorizationService.customAuthLogics.register("is-participant-approved") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val memberId = authInfo["member"] as? IdType
            if (resourceId != null && memberId != null)
                taskService.isParticipantApproved(resourceId, memberId)
            else false
        }
        authorizationService.customAuthLogics.register("is-space-admin-of-task") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val spaceId = authInfo["space"] as? IdType
            val spaceQueryAndAdmin =
                if (spaceId != null) {
                    spaceService.isSpaceAdmin(spaceId, userId)
                } else false
            val isAdminForInstance =
                if (resourceId != null) {
                    val spaceId = taskService.getTaskSpaceId(resourceId)
                    if (spaceId != null) {
                        spaceService.isSpaceAdmin(spaceId, userId)
                    } else false
                } else false
            spaceQueryAndAdmin || isAdminForInstance
        }
        authorizationService.customAuthLogics.register("is-team-admin-of-task") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val teamId = authInfo["team"] as? IdType
            val teamQueryAndAdmin =
                if (teamId != null) {
                    teamService.isTeamAdmin(teamId, userId)
                } else false
            val isAdminForInstance =
                if (resourceId != null) {
                    val teamId = taskService.getTaskTeamId(resourceId)
                    if (teamId != null) {
                        teamService.isTeamAdmin(teamId, userId)
                    } else false
                } else false
            teamQueryAndAdmin || isAdminForInstance
        }
        authorizationService.customAuthLogics.register("is-task-in-space") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            if (resourceId != null) taskService.getTaskSpaceId(resourceId) != null else false
        }
        authorizationService.customAuthLogics.register("is-task-in-team") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            if (resourceId != null) taskService.getTaskTeamId(resourceId) != null else false
        }
        authorizationService.customAuthLogics.register("task-has-any-participant") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            if (resourceId == null) false else taskService.taskHasAnyParticipant(resourceId)
        }
        authorizationService.customAuthLogics.register("task-has-any-submission") {
            _: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            if (resourceId == null) false
            else taskSubmissionService.taskHasAnySubmission(resourceId)
        }
        authorizationService.customAuthLogics.register("is-enumerating-owned-tasks") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            _: IdType?,
            authInfo: Map<String, Any>,
            _: IdGetter?,
            _: Any?,
            ->
            val ownerId = authInfo["owner"] as? IdType
            if (ownerId == null) {
                false
            } else {
                ownerId == userId
            }
        }
    }

    @Guard("delete", "task")
    override fun deleteTask(@ResourceId taskId: Long): ResponseEntity<DeleteTask200ResponseDTO> {
        taskService.deleteTask(taskId)
        return ResponseEntity.ok(DeleteTask200ResponseDTO(200, "OK"))
    }

    @Guard("remove-participant", "task")
    override fun deleteTaskParticipant(
        @ResourceId taskId: Long,
        @AuthInfo("member") member: Long
    ): ResponseEntity<GetTask200ResponseDTO> {
        taskService.removeTaskParticipant(taskId, member)
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(
            GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Guard("query", "task")
    override fun getTask(
        @ResourceId taskId: Long,
        queryJoinability: Boolean,
        querySubmittability: Boolean
    ): ResponseEntity<GetTask200ResponseDTO> {
        val taskDTO = taskService.getTaskDto(taskId, queryJoinability, querySubmittability)
        return ResponseEntity.ok(
            GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Guard("enumerate-participants", "task")
    override fun getTaskParticipants(
        @ResourceId taskId: Long
    ): ResponseEntity<GetTaskParticipants200ResponseDTO> {
        val participants = taskService.getTaskParticipantDtos(taskId)
        return ResponseEntity.ok(
            GetTaskParticipants200ResponseDTO(
                200,
                GetTaskParticipants200ResponseDataDTO(participants),
                "OK"
            )
        )
    }

    @Guard("enumerate-submissions", "task")
    override fun getTaskSubmissions(
        @ResourceId taskId: Long,
        @AuthInfo("member") member: Long?,
        allVersions: Boolean,
        queryReview: Boolean,
        reviewed: Boolean?,
        pageSize: Int,
        pageStart: Long?,
        sortBy: String,
        sortOrder: String
    ): ResponseEntity<GetTaskSubmissions200ResponseDTO> {
        val by =
            when (sortBy) {
                "createdAt" -> TaskSubmissionService.TaskSubmissionSortBy.CREATED_AT
                "updatedAt" -> TaskSubmissionService.TaskSubmissionSortBy.UPDATED_AT
                else -> throw IllegalArgumentException("Invalid sortBy: $sortBy")
            }
        val order =
            when (sortOrder) {
                "asc" -> SortDirection.ASCENDING
                "desc" -> SortDirection.DESCENDING
                else -> throw IllegalArgumentException("Invalid sortOrder: $sortOrder")
            }
        val (dtos, page) =
            taskSubmissionService.enumerateSubmissions(
                taskId = taskId,
                member = member,
                allVersions = allVersions,
                queryReview = queryReview,
                reviewed = reviewed,
                pageSize = pageSize,
                pageStart = pageStart,
                sortBy = by,
                sortOrder = order,
            )
        return ResponseEntity.ok(
            GetTaskSubmissions200ResponseDTO(
                200,
                GetTaskSubmissions200ResponseDataDTO(dtos, page),
                "OK"
            )
        )
    }

    @Guard("enumerate", "task")
    override fun getTasks(
        @AuthInfo("space") space: Long?,
        @AuthInfo("team") team: Int?,
        @AuthInfo("approved") approved: Boolean?,
        @AuthInfo("owner") owner: Long?,
        pageSize: Int,
        pageStart: Long?,
        sortBy: String,
        sortOrder: String,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        keywords: String?,
    ): ResponseEntity<GetTasks200ResponseDTO> {
        val by =
            when (sortBy) {
                "createdAt" -> TaskService.TasksSortBy.CREATED_AT
                "updatedAt" -> TaskService.TasksSortBy.UPDATED_AT
                "deadline" -> TaskService.TasksSortBy.DEADLINE
                else -> throw IllegalArgumentException("Invalid sortBy: $sortBy")
            }
        val order =
            when (sortOrder) {
                "asc" -> SortDirection.ASCENDING
                "desc" -> SortDirection.DESCENDING
                else -> throw IllegalArgumentException("Invalid sortOrder: $sortOrder")
            }
        val (taskSummaryDTOs, page) =
            taskService.enumerateTasks(
                space = space,
                team = team,
                approved = approved,
                owner = owner,
                keywords = keywords,
                pageSize = pageSize,
                pageStart = pageStart,
                sortBy = by,
                sortOrder = order,
                queryJoinability = queryJoinability,
                querySubmittability = querySubmittability,
            )
        return ResponseEntity.ok(
            GetTasks200ResponseDTO(200, GetTasks200ResponseDataDTO(taskSummaryDTOs, page), "OK")
        )
    }

    @Guard("modify", "task")
    override fun patchTask(
        @ResourceId taskId: Long,
        patchTaskRequestDTO: PatchTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        if (patchTaskRequestDTO.approved != null) {
            authorizationService.audit("modify-approved", "task", taskId)
            taskService.updateApproved(taskId, patchTaskRequestDTO.approved)
        }
        if (patchTaskRequestDTO.rejectReason != null) {
            authorizationService.audit("modify-reject-reason", "task", taskId)
            taskService.updateRejectReason(taskId, patchTaskRequestDTO.rejectReason)
        }
        if (patchTaskRequestDTO.name != null) {
            taskService.updateTaskName(taskId, patchTaskRequestDTO.name)
        }
        if (patchTaskRequestDTO.hasDeadline == false) {
            taskService.updateTaskDeadline(taskId, null)
        }
        if (patchTaskRequestDTO.deadline != null) {
            taskService.updateTaskDeadline(taskId, patchTaskRequestDTO.deadline.toLocalDateTime())
        }
        if (patchTaskRequestDTO.defaultDeadline != null) {
            taskService.updateTaskDefaultDeadline(taskId, patchTaskRequestDTO.defaultDeadline)
        }
        if (patchTaskRequestDTO.resubmittable != null) {
            taskService.updateTaskResubmittable(taskId, patchTaskRequestDTO.resubmittable)
        }
        if (patchTaskRequestDTO.editable != null) {
            taskService.updateTaskEditable(taskId, patchTaskRequestDTO.editable)
        }
        if (patchTaskRequestDTO.intro != null) {
            taskService.updateTaskIntro(taskId, patchTaskRequestDTO.intro)
        }
        if (patchTaskRequestDTO.description != null) {
            taskService.updateTaskDescription(taskId, patchTaskRequestDTO.description)
        }
        if (patchTaskRequestDTO.submissionSchema != null) {
            taskService.updateTaskSubmissionSchema(
                taskId,
                patchTaskRequestDTO.submissionSchema.withIndex().map {
                    TaskSubmissionSchema(
                        it.index,
                        it.value.prompt,
                        taskSubmissionService.convertTaskSubmissionEntryType(it.value.type)
                    )
                }
            )
        }
        if (patchTaskRequestDTO.rank != null) {
            taskService.updateTaskRank(taskId, patchTaskRequestDTO.rank)
        }
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(
            GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Guard("modify-membership", "task")
    override fun patchTaskMembership(
        @ResourceId taskId: Long,
        @AuthInfo("member") member: Long,
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO
    ): ResponseEntity<PatchTaskMembership200ResponseDTO> {
        val participant =
            taskService.updateTaskMembership(
                taskId,
                member,
                patchTaskMembershipRequestDTO.deadline,
                patchTaskMembershipRequestDTO.approved
            )
        return ResponseEntity.ok(
            PatchTaskMembership200ResponseDTO(
                200,
                PatchTaskMembership200ResponseDataDTO(participant),
                "OK"
            )
        )
    }

    @Guard("modify-submission", "task")
    override fun patchTaskSubmission(
        @ResourceId taskId: Long,
        @AuthInfo("member") member: Long,
        version: Int,
        postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        val contents = postTaskSubmissionRequestInnerDTO.toEntryList()
        val submissions =
            taskSubmissionService.modifySubmission(
                taskId,
                member,
                authenticationService.getCurrentUserId(),
                version,
                contents
            )
        return ResponseEntity.ok(
            PostTaskSubmission200ResponseDTO(
                200,
                PostTaskSubmission200ResponseDataDTO(submissions),
                "OK"
            )
        )
    }

    @Guard("create", "task")
    override fun postTask(
        postTaskRequestDTO: PostTaskRequestDTO
    ): ResponseEntity<GetTask200ResponseDTO> {
        val taskId =
            taskService.createTask(
                name = postTaskRequestDTO.name,
                submitterType =
                    taskService.convertTaskSubmitterType(postTaskRequestDTO.submitterType),
                deadline = postTaskRequestDTO.deadline?.toLocalDateTime(),
                defaultDeadline = postTaskRequestDTO.defaultDeadline,
                resubmittable = postTaskRequestDTO.resubmittable,
                editable = postTaskRequestDTO.editable,
                intro = postTaskRequestDTO.intro,
                description = postTaskRequestDTO.description,
                submissionSchema =
                    postTaskRequestDTO.submissionSchema.withIndex().map {
                        TaskSubmissionSchema(
                            it.index,
                            it.value.prompt,
                            taskSubmissionService.convertTaskSubmissionEntryType(it.value.type)
                        )
                    },
                creatorId = authenticationService.getCurrentUserId(),
                teamId = postTaskRequestDTO.team,
                spaceId = postTaskRequestDTO.space,
                rank = postTaskRequestDTO.rank
            )
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(
            GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Guard("add-participant", "task")
    override fun postTaskParticipant(
        @ResourceId taskId: Long,
        @AuthInfo("member") member: Long,
        @AuthInfo("deadline") deadline: Long?
    ): ResponseEntity<GetTask200ResponseDTO> {
        if (deadline != null) {
            taskService.addTaskParticipant(taskId, member, deadline.toLocalDateTime(), true)
        } else {
            taskService.addTaskParticipant(taskId, member, null, false)
        }
        val taskDTO = taskService.getTaskDto(taskId)
        return ResponseEntity.ok(
            GetTask200ResponseDTO(200, GetTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Guard("submit", "task")
    override fun postTaskSubmission(
        @ResourceId taskId: Long,
        @AuthInfo("member") member: Long,
        postTaskSubmissionRequestInnerDTO: List<PostTaskSubmissionRequestInnerDTO>
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        val contents = postTaskSubmissionRequestInnerDTO.toEntryList()
        val submissions =
            taskSubmissionService.submitTask(
                taskId,
                member,
                authenticationService.getCurrentUserId(),
                contents
            )
        return ResponseEntity.ok(
            PostTaskSubmission200ResponseDTO(
                200,
                PostTaskSubmission200ResponseDataDTO(submissions),
                "OK"
            )
        )
    }

    @Guard("create-submission-review", "task")
    override fun postTaskSubmissionReview(
        @AuthInfo("submission") submissionId: Long,
        postTaskSubmissionReviewRequestDTO: PostTaskSubmissionReviewRequestDTO
    ): ResponseEntity<PostTaskSubmissionReview200ResponseDTO> {
        val hasUpgradedParticipantRank =
            taskSubmissionReviewService.createReview(
                submissionId = submissionId,
                accepted = postTaskSubmissionReviewRequestDTO.accepted,
                score = postTaskSubmissionReviewRequestDTO.score,
                comment = postTaskSubmissionReviewRequestDTO.comment,
            )
        val submissionDTO = taskSubmissionService.getSubmissionDTO(submissionId, queryReview = true)
        return ResponseEntity.ok(
            PostTaskSubmissionReview200ResponseDTO(
                200,
                PostTaskSubmissionReview200ResponseDataDTO(
                    submissionDTO,
                    hasUpgradedParticipantRank
                ),
                "OK"
            )
        )
    }

    @Guard("modify-submission-review", "task")
    override fun patchTaskSubmissionReview(
        @AuthInfo("submission") submissionId: Long,
        patchTaskSubmissionReviewRequestDTO: PatchTaskSubmissionReviewRequestDTO
    ): ResponseEntity<PostTaskSubmissionReview200ResponseDTO> {
        var hasUpgradedParticipantRank = false
        if (patchTaskSubmissionReviewRequestDTO.accepted != null) {
            hasUpgradedParticipantRank =
                taskSubmissionReviewService.updateReviewAccepted(
                    submissionId = submissionId,
                    accepted = patchTaskSubmissionReviewRequestDTO.accepted
                )
        }
        if (patchTaskSubmissionReviewRequestDTO.score != null) {
            taskSubmissionReviewService.updateReviewScore(
                submissionId = submissionId,
                score = patchTaskSubmissionReviewRequestDTO.score
            )
        }
        if (patchTaskSubmissionReviewRequestDTO.comment != null) {
            taskSubmissionReviewService.updateReviewComment(
                submissionId = submissionId,
                comment = patchTaskSubmissionReviewRequestDTO.comment
            )
        }
        val submissionDTO = taskSubmissionService.getSubmissionDTO(submissionId, queryReview = true)
        return ResponseEntity.ok(
            PostTaskSubmissionReview200ResponseDTO(
                200,
                PostTaskSubmissionReview200ResponseDataDTO(
                    submissionDTO,
                    hasUpgradedParticipantRank
                ),
                "OK"
            )
        )
    }

    @Guard("delete-submission-review", "task")
    override fun deleteTaskSubmissionReview(
        @AuthInfo("submission") submissionId: Long,
    ): ResponseEntity<Unit> {
        taskSubmissionReviewService.deleteReview(submissionId)
        return ResponseEntity.ok().build()
    }
}
