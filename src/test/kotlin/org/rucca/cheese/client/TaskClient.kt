package org.rucca.cheese.client

import java.time.LocalDateTime
import kotlin.math.floor
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.toDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Client for creating and managing test tasks. Provides reusable methods for task-related
 * operations in tests.
 */
@Service
class TaskClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun testTaskName(): String {
        return "Test Task (${floor(Math.random() * 10000000000).toLong()})"
    }

    fun testTaskIntro(): String {
        return "This is a test task."
    }

    fun testTaskDescription(): String {
        return "A lengthy text. ".repeat(100)
    }

    fun testTaskDeadline(): Long {
        return LocalDateTime.now().plusDays(7).toEpochMilli()
    }

    fun testTaskDefaultDeadline(): Long {
        return LocalDateTime.now().plusDays(7).toEpochMilli()
    }

    fun testSubmissionSchema(): List<TaskSubmissionSchemaEntryDTO> {
        return listOf(
            TaskSubmissionSchemaEntryDTO(prompt = "Text Entry", type = TaskSubmissionTypeDTO.TEXT)
        )
    }

    /** Creates a task and returns its ID. */
    fun createTask(
        webTestClient: WebTestClient,
        token: String,
        name: String = testTaskName(),
        submitterType: TaskSubmitterType = TaskSubmitterType.USER,
        registrationStartAt: Long? = null,
        deadline: Long? = testTaskDeadline(),
        defaultDeadline: Long? = testTaskDefaultDeadline(),
        resubmittable: Boolean = true,
        editable: Boolean = true,
        intro: String = testTaskIntro(),
        description: String = testTaskDescription(),
        submissionSchema: List<TaskSubmissionSchemaEntryDTO> = testSubmissionSchema(),
        spaceId: IdType,
        categoryId: IdType? = null,
        rank: Int? = null,
        topics: List<IdType> = emptyList(),
        expectedStatus: Int = HttpStatus.OK.value(),
        maxParticipants: Int = 100,
        enableRanking: Boolean = false,
    ): IdType? {
        val requestDTO =
            PostTaskRequestDTO(
                name = name,
                submitterType = submitterType.toDTO(),
                registrationStartAt = registrationStartAt,
                deadline = deadline,
                defaultDeadline = defaultDeadline,
                resubmittable = resubmittable,
                editable = editable,
                intro = intro,
                description = description,
                submissionSchema = submissionSchema,
                space = spaceId,
                categoryId = categoryId,
                rank = rank,
                topics = topics,
            )

        val exchange =
            webTestClient
                .post()
                .uri("/tasks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()

        if (expectedStatus != HttpStatus.OK.value()) {
            exchange.expectStatus().isEqualTo(expectedStatus)
            return null
        }

        return try {
            val responseDTO =
                exchange
                    .expectStatus()
                    .isOk()
                    .expectBody<PatchTask200ResponseDTO>()
                    .returnResult()
                    .responseBody!!

            val taskId = responseDTO.data.task.id
            logger.info("Created task: $taskId in space: $spaceId")
            taskId
        } catch (e: Exception) {
            logger.error("Failed to create task in space $spaceId: ${e.message}")
            logger.error("Request was: name=$name, categoryId=$categoryId")
            null
        }
    }

    /** Approves a task. */
    fun approveTask(webTestClient: WebTestClient, taskId: IdType, token: String) {
        val requestDTO = PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED)

        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk()
    }

    /** Adds a user participant to a task. */
    fun addParticipantUser(
        webTestClient: WebTestClient,
        token: String,
        taskId: IdType,
        userId: IdType,
        email: String = "test@example.com",
    ): IdType {
        val requestDTO = PostTaskParticipantRequestDTO(email = email)

        val responseDTO =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants?member=$userId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody!!

        return responseDTO.data.participant!!.id
    }

    /** Adds a team participant to a task. */
    fun addParticipantTeam(
        webTestClient: WebTestClient,
        token: String,
        taskId: IdType,
        teamId: IdType,
        email: String = "test@example.com",
    ): IdType {
        val requestDTO = PostTaskParticipantRequestDTO(email = email)

        val responseDTO =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants?member=$teamId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody!!

        return responseDTO.data.participant!!.id
    }

    /** Approves a task participant. */
    fun approveTaskParticipant(
        webTestClient: WebTestClient,
        token: String,
        taskId: IdType,
        participantId: IdType,
    ) {
        val requestDTO = PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED)

        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$participantId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk()
    }

    /** Submits a task for a user participant. */
    fun submitTaskUser(
        webTestClient: WebTestClient,
        token: String,
        taskId: IdType,
        participantId: IdType,
        content: List<Any>,
    ): IdType {
        val responseDTO =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants/$participantId/submissions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(content)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PostTaskSubmission200ResponseDTO>()
                .returnResult()
                .responseBody!!

        val submissionId = responseDTO.data.submission.id
        logger.info("Created submission: $submissionId for task: $taskId")
        return submissionId
    }

    /** Join a task as a participant. */
    fun joinTask(
        webTestClient: WebTestClient,
        taskId: IdType,
        token: String,
        userId: IdType,
    ): IdType {
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com")

        val responseDTO =
            webTestClient
                .post()
                .uri { builder ->
                    builder.path("/tasks/$taskId/participants").queryParam("member", userId).build()
                }
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PostTaskParticipant200ResponseDTO>()
                .returnResult()
                .responseBody!!

        val participantId = responseDTO.data.participant!!.id
        logger.info("User joined task: $taskId as participant: $participantId")
        return participantId
    }

    /** Submit a task. */
    fun submitTask(
        webTestClient: WebTestClient,
        taskId: IdType,
        token: String,
        participantId: IdType,
        content: String = "Test submission content",
    ): IdType {
        val submissionContent = listOf(mapOf("text" to content))

        val responseDTO =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants/$participantId/submissions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(submissionContent)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PostTaskSubmission200ResponseDTO>()
                .returnResult()
                .responseBody!!

        val submissionId = responseDTO.data.submission.id
        logger.info("Submitted task: $taskId, submission: $submissionId")
        return submissionId
    }
}
