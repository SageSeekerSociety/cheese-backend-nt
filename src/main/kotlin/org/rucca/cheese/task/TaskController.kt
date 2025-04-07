/*
 *  Description: This file defines the TaskController class.
 *               It provides endpoints of /tasks.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      HuanCheng65
 *      nameisyui
 *
 */

package org.rucca.cheese.task

import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.hibernate.query.SortDirection
import org.rucca.cheese.api.TasksApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.AuthContext
import org.rucca.cheese.auth.spring.ResourceId
import org.rucca.cheese.common.helper.toLocalDateTime
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.common.persistent.convert
import org.rucca.cheese.llm.error.ConversationNotFoundError
import org.rucca.cheese.model.*
import org.rucca.cheese.task.option.TaskEnumerateOptions
import org.rucca.cheese.task.option.TaskQueryOptions
import org.rucca.cheese.user.services.UserService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

fun List<TaskSubmissionContentDTO>.toEntryList() = map {
    if (it.text != null) {
        TaskSubmissionService.TaskSubmissionEntry.Text(it.text)
    } else if (it.attachmentId != null) {
        TaskSubmissionService.TaskSubmissionEntry.Attachment(it.attachmentId)
    } else {
        throw IllegalArgumentException("Invalid TaskSubmissionContentDTO: $it")
    }
}

@RestController
@UseNewAuth
class TaskController(
    private val taskService: TaskService,
    private val taskSubmissionService: TaskSubmissionService,
    private val taskSubmissionReviewService: TaskSubmissionReviewService,
    private val jwtService: JwtService,
    private val taskTopicsService: TaskTopicsService,
    private val taskMembershipService: TaskMembershipService,
    private val taskAIAdviceService: TaskAIAdviceService,
    private val userService: UserService,
) : TasksApi {

    @Auth("task:delete:task")
    override suspend fun deleteTask(@ResourceId taskId: Long): ResponseEntity<CommonResponseDTO> {
        taskService.deleteTask(taskId)
        return ResponseEntity.ok(CommonResponseDTO(200, "OK"))
    }

    @Auth("task:delete:participant")
    override suspend fun deleteTaskParticipant(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            taskMembershipService.removeTaskParticipant(taskId, participantId)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth("task:delete:participant")
    override suspend fun deleteTaskParticipantByMember(
        @ResourceId taskId: Long,
        @AuthContext("memberId") member: Long,
    ): ResponseEntity<Unit> {
        withContext(Dispatchers.IO) {
            taskMembershipService.removeTaskParticipantByMemberId(taskId, member)
        }
        return ResponseEntity.noContent().build()
    }

    @Auth("task:query:task")
    override suspend fun getTask(
        @ResourceId taskId: Long,
        querySpace: Boolean,
        queryTeam: Boolean,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        queryJoined: Boolean,
        queryUserDeadline: Boolean,
        queryTopics: Boolean,
    ): ResponseEntity<GetTask200ResponseDTO> {
        val queryOptions =
            TaskQueryOptions(
                querySpace = querySpace,
                queryJoinability = queryJoinability,
                querySubmittability = querySubmittability,
                queryJoined = queryJoined,
                queryTopics = queryTopics,
                queryUserDeadline = queryUserDeadline,
            )
        val taskDTO = taskService.getTaskDto(taskId, queryOptions)
        val participationInfoDTO =
            taskMembershipService.getUserParticipationInfo(
                taskId = taskId,
                userId = jwtService.getCurrentUserId(),
            )
        return ResponseEntity.ok(
            GetTask200ResponseDTO(
                code = 200,
                data = GetTask200ResponseDataDTO(taskDTO, participationInfoDTO),
                message = "OK",
            )
        )
    }

    @Auth("task:enumerate:participant")
    override suspend fun getTaskParticipants(
        @ResourceId taskId: Long,
        approved: ApproveTypeDTO?,
        queryRealNameInfo: Boolean,
    ): ResponseEntity<GetTaskParticipants200ResponseDTO> {
        val approveType = approved?.convert()
        val participants =
            taskMembershipService.getTaskMembershipDTOs(taskId, approveType, queryRealNameInfo)
        return ResponseEntity.ok(
            GetTaskParticipants200ResponseDTO(
                200,
                GetTaskParticipants200ResponseDataDTO(participants),
                "OK",
            )
        )
    }

    @Auth("task:enumerate:submission")
    override suspend fun getTaskSubmissions(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        allVersions: Boolean,
        queryReview: Boolean,
        reviewed: Boolean?,
        pageSize: Int,
        pageStart: Long?,
        sortBy: String,
        sortOrder: String,
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
                participantId = participantId,
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
                "OK",
            )
        )
    }

    @Auth("task:enumerate:task")
    override suspend fun getTasks(
        @AuthContext("spaceId") space: Long,
        @AuthContext("categoryId") categoryId: Long?,
        @AuthContext("approved") approved: ApproveTypeDTO?,
        @AuthContext("queryOwner") owner: Long?,
        @AuthContext("queryJoined") joined: Boolean?,
        topics: List<Long>?,
        pageSize: Int,
        pageStart: Long?,
        sortBy: String,
        sortOrder: String,
        querySpace: Boolean,
        queryTeam: Boolean,
        queryJoinability: Boolean,
        querySubmittability: Boolean,
        queryJoined: Boolean,
        queryUserDeadline: Boolean,
        queryTopics: Boolean,
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
        val enumerateOptions =
            TaskEnumerateOptions(
                space = space,
                categoryId = categoryId,
                approved = approved?.convert(),
                owner = owner,
                joined = joined,
                topics = topics,
            )
        val queryOptions =
            TaskQueryOptions(
                querySpace = querySpace,
                queryJoinability = queryJoinability,
                querySubmittability = querySubmittability,
                queryJoined = queryJoined,
                queryTopics = queryTopics,
                queryUserDeadline = queryUserDeadline,
            )
        val (taskSummaryDTOs, page) =
            taskService.enumerateTasks(
                enumerateOptions = enumerateOptions,
                keywords = keywords,
                pageSize = pageSize,
                pageStart = pageStart,
                sortBy = by,
                sortOrder = order,
                queryOptions = queryOptions,
            )
        return ResponseEntity.ok(
            GetTasks200ResponseDTO(200, GetTasks200ResponseDataDTO(taskSummaryDTOs, page), "OK")
        )
    }

    @Auth("task:modify:task")
    override suspend fun patchTask(
        @ResourceId taskId: Long,
        @AuthContext("approved", field = "approved")
        @AuthContext("rejectReason", field = "rejectReason")
        patchTaskRequestDTO: PatchTaskRequestDTO,
    ): ResponseEntity<PatchTask200ResponseDTO> {
        if (patchTaskRequestDTO.approved != null) {
            taskService.updateApproved(taskId, patchTaskRequestDTO.approved.convert())
        }
        if (patchTaskRequestDTO.rejectReason != null) {
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
        if (patchTaskRequestDTO.hasParticipantLimit == false) {
            taskService.updateTaskParticipantLimit(taskId, null)
        }
        if (patchTaskRequestDTO.participantLimit != null) {
            taskService.updateTaskParticipantLimit(taskId, patchTaskRequestDTO.participantLimit)
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
                        taskSubmissionService.convertTaskSubmissionEntryType(it.value.type),
                    )
                },
            )
        }
        if (patchTaskRequestDTO.rank != null) {
            taskService.updateTaskRank(taskId, patchTaskRequestDTO.rank)
        }
        if (patchTaskRequestDTO.topics != null) {
            taskTopicsService.updateTaskTopics(taskId, patchTaskRequestDTO.topics)
        }
        if (patchTaskRequestDTO.requireRealName != null) {
            taskService.updateTaskRequireRealName(taskId, patchTaskRequestDTO.requireRealName)
        }
        if (patchTaskRequestDTO.categoryId != null) {
            taskService.updateTaskCategory(taskId, patchTaskRequestDTO.categoryId)
        }
        val taskDTO = taskService.getTaskDto(taskId, TaskQueryOptions.MAXIMUM)
        return ResponseEntity.ok(
            PatchTask200ResponseDTO(200, PatchTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Auth("task:modify:participant")
    override suspend fun patchTaskParticipant(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        @AuthContext("approved", "approved")
        @AuthContext("deadline", "deadline")
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO,
    ): ResponseEntity<GetTaskParticipant200ResponseDTO> {
        val participant =
            withContext(Dispatchers.IO) {
                taskMembershipService.updateTaskMembership(
                    participantId,
                    patchTaskMembershipRequestDTO,
                )
            }
        return ResponseEntity.ok(
            GetTaskParticipant200ResponseDTO(
                200,
                GetTaskParticipant200ResponseDataDTO(participant),
                "OK",
            )
        )
    }

    @Auth("task:modify:participant")
    override suspend fun patchTaskMembershipByMember(
        @ResourceId taskId: Long,
        @AuthContext("memberId") member: Long,
        @AuthContext("approved", "approved")
        @AuthContext("deadline", "deadline")
        patchTaskMembershipRequestDTO: PatchTaskMembershipRequestDTO,
    ): ResponseEntity<PatchTaskMembershipByMember200ResponseDTO> {
        withContext(Dispatchers.IO) {
            taskMembershipService.updateTaskMembership(
                taskId,
                member,
                patchTaskMembershipRequestDTO,
            )
        }
        val participants = taskMembershipService.getTaskMembershipDTOs(taskId, null)
        return ResponseEntity.ok(
            PatchTaskMembershipByMember200ResponseDTO(
                200,
                PatchTaskMembershipByMember200ResponseDataDTO(participants),
                "OK",
            )
        )
    }

    @Auth("task:modify:submission")
    override suspend fun patchTaskSubmission(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        version: Int,
        taskSubmissionContentDTO: Flow<TaskSubmissionContentDTO>,
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        val contents = taskSubmissionContentDTO.toList().toEntryList()
        val submissions =
            taskSubmissionService.modifySubmission(
                taskId,
                participantId,
                jwtService.getCurrentUserId(),
                version,
                contents,
            )
        return ResponseEntity.ok(
            PostTaskSubmission200ResponseDTO(
                200,
                PostTaskSubmission200ResponseDataDTO(submissions),
                "OK",
            )
        )
    }

    @Auth("task:create:task")
    override suspend fun postTask(
        postTaskRequestDTO: PostTaskRequestDTO
    ): ResponseEntity<PatchTask200ResponseDTO> {
        val taskId =
            taskService.createTask(
                name = postTaskRequestDTO.name,
                submitterType =
                    taskService.convertTaskSubmitterType(postTaskRequestDTO.submitterType),
                deadline = postTaskRequestDTO.deadline?.toLocalDateTime(),
                participantLimit = postTaskRequestDTO.participantLimit,
                defaultDeadline = postTaskRequestDTO.defaultDeadline ?: 30,
                resubmittable = postTaskRequestDTO.resubmittable,
                editable = postTaskRequestDTO.editable,
                intro = postTaskRequestDTO.intro,
                description = postTaskRequestDTO.description,
                submissionSchema =
                    postTaskRequestDTO.submissionSchema.withIndex().map {
                        TaskSubmissionSchema(
                            it.index,
                            it.value.prompt,
                            taskSubmissionService.convertTaskSubmissionEntryType(it.value.type),
                        )
                    },
                creatorId = jwtService.getCurrentUserId(),
                spaceId = postTaskRequestDTO.space,
                categoryId = postTaskRequestDTO.categoryId,
                rank = postTaskRequestDTO.rank,
                requireRealName = postTaskRequestDTO.requireRealName ?: false,
            )
        taskTopicsService.updateTaskTopics(taskId, postTaskRequestDTO.topics ?: emptyList())
        val taskDTO = taskService.getTaskDto(taskId, TaskQueryOptions.MAXIMUM)
        return ResponseEntity.ok(
            PatchTask200ResponseDTO(200, PatchTask200ResponseDataDTO(taskDTO), "OK")
        )
    }

    @Auth("task:create:participant")
    override suspend fun postTaskParticipant(
        @ResourceId taskId: Long,
        @AuthContext("memberId") member: Long,
        @AuthContext("deadline", field = "deadline")
        postTaskParticipantRequestDTO: PostTaskParticipantRequestDTO,
    ): ResponseEntity<PostTaskParticipant200ResponseDTO> {
        val approved =
            if (postTaskParticipantRequestDTO.deadline != null) ApproveType.APPROVED
            else ApproveType.NONE
        val participant =
            taskMembershipService.addTaskParticipant(
                taskId,
                member,
                postTaskParticipantRequestDTO.deadline?.toLocalDateTime(),
                approved,
                postTaskParticipantRequestDTO.email,
                postTaskParticipantRequestDTO.phone,
                postTaskParticipantRequestDTO.applyReason,
                postTaskParticipantRequestDTO.personalAdvantage,
                postTaskParticipantRequestDTO.remark,
            )
        return ResponseEntity.ok(
            PostTaskParticipant200ResponseDTO(
                200,
                PostTaskParticipant200ResponseDataDTO(participant),
                "OK",
            )
        )
    }

    @Auth("task:create:submission")
    override suspend fun postTaskSubmission(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        taskSubmissionContentDTO: Flow<TaskSubmissionContentDTO>,
    ): ResponseEntity<PostTaskSubmission200ResponseDTO> {
        val contents = taskSubmissionContentDTO.toList().toEntryList()
        val submissions =
            taskSubmissionService.submitTask(
                taskId,
                participantId,
                jwtService.getCurrentUserId(),
                contents,
            )
        return ResponseEntity.ok(
            PostTaskSubmission200ResponseDTO(
                200,
                PostTaskSubmission200ResponseDataDTO(submissions),
                "OK",
            )
        )
    }

    @Auth("task:create:submission-review")
    override suspend fun postTaskSubmissionReview(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        @AuthContext("submissionId") submissionId: Long,
        postTaskSubmissionReviewRequestDTO: PostTaskSubmissionReviewRequestDTO,
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
                    hasUpgradedParticipantRank,
                ),
                "OK",
            )
        )
    }

    @Auth("task:modify:submission-review")
    override suspend fun patchTaskSubmissionReview(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        @AuthContext("submissionId") submissionId: Long,
        patchTaskSubmissionReviewRequestDTO: PatchTaskSubmissionReviewRequestDTO,
    ): ResponseEntity<PostTaskSubmissionReview200ResponseDTO> {
        var hasUpgradedParticipantRank = false
        if (patchTaskSubmissionReviewRequestDTO.accepted != null) {
            hasUpgradedParticipantRank =
                taskSubmissionReviewService.updateReviewAccepted(
                    submissionId = submissionId,
                    accepted = patchTaskSubmissionReviewRequestDTO.accepted,
                )
        }
        if (patchTaskSubmissionReviewRequestDTO.score != null) {
            taskSubmissionReviewService.updateReviewScore(
                submissionId = submissionId,
                score = patchTaskSubmissionReviewRequestDTO.score,
            )
        }
        if (patchTaskSubmissionReviewRequestDTO.comment != null) {
            taskSubmissionReviewService.updateReviewComment(
                submissionId = submissionId,
                comment = patchTaskSubmissionReviewRequestDTO.comment,
            )
        }
        val submissionDTO = taskSubmissionService.getSubmissionDTO(submissionId, queryReview = true)
        return ResponseEntity.ok(
            PostTaskSubmissionReview200ResponseDTO(
                200,
                PostTaskSubmissionReview200ResponseDataDTO(
                    submissionDTO,
                    hasUpgradedParticipantRank,
                ),
                "OK",
            )
        )
    }

    @Auth("task:delete:submission-review")
    override suspend fun deleteTaskSubmissionReview(
        @ResourceId taskId: Long,
        @AuthContext("participantId") participantId: Long,
        @AuthContext("submissionId") submissionId: Long,
    ): ResponseEntity<Unit> {
        taskSubmissionReviewService.deleteReview(submissionId)
        return ResponseEntity.ok().build()
    }

    @Auth("task:query:ai-advice")
    override suspend fun getTaskAiAdvice(
        @ResourceId taskId: IdType
    ): ResponseEntity<GetTaskAiAdvice200ResponseDTO> {
        return ResponseEntity.ok(
            GetTaskAiAdvice200ResponseDTO(200, taskAIAdviceService.getTaskAIAdvice(taskId), "OK")
        )
    }

    @Auth("task:query:ai-advice")
    override suspend fun getTaskAiAdviceStatus(
        @ResourceId taskId: IdType
    ): ResponseEntity<GetTaskAiAdviceStatus200ResponseDTO> {
        val data = taskAIAdviceService.getTaskAIAdviceStatus(taskId)
        return ResponseEntity.ok(GetTaskAiAdviceStatus200ResponseDTO(200, data, "OK"))
    }

    @Auth("task:create:ai-advice")
    override suspend fun requestTaskAiAdvice(
        @ResourceId taskId: IdType
    ): ResponseEntity<RequestTaskAiAdvice200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val data = taskAIAdviceService.requestTaskAIAdvice(taskId, userId)
        return ResponseEntity.ok(RequestTaskAiAdvice200ResponseDTO(200, data, "OK"))
    }

    /**
     * Streams a research advice conversation (supports historical context).
     *
     * @param taskId Task ID.
     * @param question User question.
     * @param section Optional, specific section of interest.
     * @param index Optional, index within the section.
     * @param conversationId Optional, conversation ID to continue a specific conversation.
     * @param parentId Optional, message ID to continue a specific message.
     * @param modelType Optional, the model type used.
     * @return A stream of AI responses and related information.
     */
    @Auth("task:create:ai-advice")
    @GetMapping(
        "/tasks/{taskId}/ai-advice/conversations/stream",
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE],
    )
    suspend fun streamTaskAiAdviceConversation(
        @PathVariable @ResourceId taskId: IdType,
        @RequestParam question: String,
        @RequestParam(required = false) section: String? = null,
        @RequestParam(required = false) index: Int? = null,
        @RequestParam(required = false)
        @AuthContext("conversationId")
        conversationId: String? = null,
        @RequestParam(required = false) parentId: IdType? = null,
        @RequestParam(required = false) modelType: String? = null,
        response: HttpServletResponse,
    ): Flow<String> {
        response.setHeader("X-Accel-Buffering", "no")
        response.setHeader("Cache-Control", "no-cache")

        val userId = jwtService.getCurrentUserId()
        val userDTO = userService.getUserDto(userId)
        val context =
            if (section != null) {
                TaskAIAdviceConversationContextDTO(
                    TaskAIAdviceConversationContextDTO.Section.valueOf(section),
                    index,
                )
            } else {
                null
            }

        return taskAIAdviceService.streamConversation(
            taskId = taskId,
            userId = userId,
            question = question,
            context = context,
            conversationId = conversationId,
            parentId = parentId,
            modelType = modelType,
            userNickname = userDTO.nickname,
        )
    }

    /**
     * Creates a research advice conversation (supports historical context).
     *
     * @param taskId Task ID.
     * @param createTaskAIAdviceConversationRequestDTO Request object containing the question,
     *   context, conversation ID, and model type.
     * @return Conversation DTO and quota information.
     */
    @Auth("task:create:ai-advice")
    override suspend fun createTaskAiAdviceConversation(
        @ResourceId taskId: IdType,
        @RequestBody
        createTaskAIAdviceConversationRequestDTO: CreateTaskAIAdviceConversationRequestDTO,
    ): ResponseEntity<CreateTaskAiAdviceConversation200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val userDTO = userService.getUserDto(userId)
        val context =
            createTaskAIAdviceConversationRequestDTO.context?.let {
                TaskAIAdviceConversationContextDTO(
                    TaskAIAdviceConversationContextDTO.Section.valueOf(it.section.toString()),
                    it.index,
                )
            }

        val (conversation, quota) =
            if (createTaskAIAdviceConversationRequestDTO.conversationId != null) {
                taskAIAdviceService.continueConversation(
                    conversationId = createTaskAIAdviceConversationRequestDTO.conversationId,
                    taskId = taskId,
                    userId = userId,
                    question = createTaskAIAdviceConversationRequestDTO.question,
                    context = context,
                    parentId = createTaskAIAdviceConversationRequestDTO.parentId,
                    modelType = createTaskAIAdviceConversationRequestDTO.modelType,
                    userNickname = userDTO.nickname,
                )
            } else {
                taskAIAdviceService.startNewConversation(
                    taskId = taskId,
                    userId = userId,
                    question = createTaskAIAdviceConversationRequestDTO.question,
                    context = context,
                    modelType = createTaskAIAdviceConversationRequestDTO.modelType,
                    userNickname = userDTO.nickname,
                )
            }

        return ResponseEntity.ok(
            CreateTaskAiAdviceConversation200ResponseDTO(
                code = 200,
                data =
                    TaskAIAdviceConversationResponseDTO(conversation = conversation, quota = quota),
                message = "success",
            )
        )
    }

    @Auth("task:query:ai-advice")
    override suspend fun getTaskAiAdviceConversationsGrouped(
        @ResourceId taskId: Long
    ): ResponseEntity<GetTaskAiAdviceConversationsGrouped200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val conversationSummaries =
            taskAIAdviceService.getConversationGroupedSummary(taskId, userId)
        return ResponseEntity.ok(
            GetTaskAiAdviceConversationsGrouped200ResponseDTO(
                200,
                GetTaskAiAdviceConversationsGrouped200ResponseDataDTO(conversationSummaries),
                "OK",
            )
        )
    }

    @Auth("task:query:ai-advice")
    override suspend fun getTaskAiAdviceConversation(
        @ResourceId taskId: Long,
        @AuthContext("conversationId") conversationId: String,
    ): ResponseEntity<GetTaskAiAdviceConversation200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val conversations = taskAIAdviceService.getConversationById(conversationId, userId)
        if (conversations.isEmpty()) {
            throw ConversationNotFoundError(conversationId)
        }
        return ResponseEntity.ok(
            GetTaskAiAdviceConversation200ResponseDTO(
                200,
                GetTaskAiAdviceConversation200ResponseDataDTO(conversations),
                "OK",
            )
        )
    }

    @Auth("task:delete:ai-advice")
    override suspend fun deleteTaskAiAdviceConversation(
        @ResourceId taskId: Long,
        @AuthContext("conversationId") conversationId: String,
    ): ResponseEntity<Unit> {
        taskAIAdviceService.deleteConversation(conversationId)
        return ResponseEntity.noContent().build()
    }

    @Auth("task:query:task")
    override suspend fun getTaskTeams(
        @ResourceId taskId: Long,
        filter: String,
    ): ResponseEntity<GetTaskTeams200ResponseDTO> {
        val teamDTOs = taskService.getTeamsForTask(taskId, filter)

        return ResponseEntity.ok(
            GetTaskTeams200ResponseDTO(
                code = 200,
                data = GetTaskTeams200ResponseDataDTO(teams = teamDTOs),
                message = "OK",
            )
        )
    }
}
