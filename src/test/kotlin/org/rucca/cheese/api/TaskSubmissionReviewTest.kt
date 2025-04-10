/*
 *  Description: It tests the feature of reviewing a task's submission.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      nameisyui
 *
 */

package org.rucca.cheese.api

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.floor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.toDTO
import org.rucca.cheese.utils.AttachmentCreatorService
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class TaskSubmissionReviewTest
@Autowired
constructor(
    private val webTestClient: WebTestClient,
    private val userCreatorService: UserCreatorService,
    private val attachmentCreatorService: AttachmentCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var participant: UserCreatorService.CreateUserResponse
    lateinit var participantToken: String
    private var attachmentId: IdType = -1
    private var spaceId: IdType = -1
    private var categoryId: IdType = -1
    private var taskId: IdType = -1
    private var submissionId: IdType = -1
    private var participantTaskMembershipId: IdType = -1

    private val randomSuffix = floor(Math.random() * 10000000000).toLong()
    private val spaceName = "Test Space ($randomSuffix)"
    private val taskName = "Test Task ($randomSuffix)"
    private val taskIntro = "This is a test task."
    private val taskDescription = "A lengthy text. ".repeat(1000)
    private val taskDeadline =
        LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val taskSubmissionSchema =
        listOf(
            TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT),
            TaskSubmissionSchemaEntryDTO("Attachment Entry", TaskSubmissionTypeDTO.FILE),
        )

    // --- Helper DTO for Error Responses ---
    // Reusing the generic error structure from previous refactors
    data class ErrorData(val type: String? = null, val id: Any? = null, val name: String? = null)

    data class ErrorDetail(val name: String, val data: ErrorData?)

    data class GenericErrorResponse(val error: ErrorDetail)

    // --- Refactored Helper Methods ---

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
    ): Pair<IdType, IdType> {
        val requestDTO =
            PostSpaceRequestDTO(
                name = spaceName,
                intro = spaceIntro,
                description = spaceDescription,
                avatarId = spaceAvatarId,
                announcements = "[]",
                taskTemplates = "[]",
            )
        val response =
            webTestClient
                .post()
                .uri("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PatchSpace200ResponseDTO>() // Assuming POST returns Patch DTO
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.space, "Space data should not be null in response")
        val createdSpace = response!!.data.space
        assertNotNull(createdSpace.id, "Created space ID cannot be null")
        assertNotNull(createdSpace.defaultCategoryId, "Default category ID cannot be null")

        val createdSpaceId = createdSpace.id
        val createdDefaultCategoryId = createdSpace.defaultCategoryId
        logger.info(
            "Created space: $createdSpaceId with default category ID: $createdDefaultCategoryId"
        )
        return Pair(createdSpaceId, createdDefaultCategoryId)
    }

    fun createTask(
        name: String,
        submitterType: TaskSubmitterType,
        deadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<TaskSubmissionSchemaEntryDTO>,
        space: IdType,
        categoryId: IdType?,
    ): IdType {
        val requestDTO =
            PostTaskRequestDTO(
                name = name,
                submitterType = submitterType.toDTO(),
                deadline = deadline,
                resubmittable = resubmittable,
                editable = editable,
                intro = intro,
                description = description,
                submissionSchema = submissionSchema,
                space = space,
                categoryId = categoryId,
                topics = emptyList(),
                rank = null,
            )

        val response =
            webTestClient
                .post()
                .uri("/tasks")
                .header("Authorization", "Bearer $creatorToken") // Assumes creator creates tasks
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PatchTask200ResponseDTO>() // Assuming POST /tasks returns this
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.task, "Task data should not be null")
        val createdTaskId = response!!.data.task.id
        assertNotNull(createdTaskId, "Created task ID should not be null")
        logger.info("Created task: $createdTaskId")
        return createdTaskId
    }

    fun joinTask(taskId: IdType, participantId: IdType, participantToken: String): IdType {
        // Assuming PostTaskParticipantRequestDTO exists, even if minimal
        val requestDTO = PostTaskParticipantRequestDTO(email = "test@example.com") // Minimal data

        val response =
            webTestClient
                .post()
                .uri { builder ->
                    builder
                        .path("/tasks/$taskId/participants")
                        .queryParam("member", participantId)
                        .build()
                }
                .header("Authorization", "Bearer $participantToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<
                    PostTaskParticipant200ResponseDTO
                >() // Expect the participant response DTO
                .returnResult()
                .responseBody

        // Assuming the DTO has data.participant.id structure based on original JSON parsing
        assertNotNull(response?.data?.participant, "Participant data should not be null")
        val taskMembershipId = response!!.data.participant!!.id
        assertNotNull(taskMembershipId, "Task Membership ID should not be null")
        logger.info("Joined task, membership ID: $taskMembershipId")
        return taskMembershipId
    }

    fun approveTaskParticipant(token: String, taskId: IdType, participantMembershipId: IdType) {
        // Assuming PatchTaskMembershipRequestDTO exists
        val requestDTO =
            PatchTaskMembershipRequestDTO(approved = ApproveTypeDTO.APPROVED) // Use enum DTO

        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$participantMembershipId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<
                GetTaskParticipant200ResponseDTO
            >() // Assuming PATCH returns the updated membership DTO
            .value { response ->
                assertNotNull(response.data.taskMembership, "Task membership data missing")
                assertEquals(ApproveTypeDTO.APPROVED, response.data.taskMembership.approved)
            }
    }

    fun submitTask(
        taskId: IdType,
        participantMembershipId: IdType,
        participantToken: String,
        attachmentId: IdType,
    ): IdType {
        // Assuming TaskSubmissionContentDTO exists for the request body elements
        val requestBody =
            listOf(
                TaskSubmissionContentDTO(text = "This is a test submission."),
                TaskSubmissionContentDTO(attachmentId = attachmentId),
            )

        val response =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants/$participantMembershipId/submissions")
                .header("Authorization", "Bearer $participantToken")
                .contentType(MediaType.APPLICATION_JSON)
                // Use bodyValue for lists; requires correct message writers configured
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PostTaskSubmission200ResponseDTO>() // Expect submission response DTO
                .returnResult()
                .responseBody

        // Assuming DTO structure data.submission.id based on original JSON parsing
        assertNotNull(response?.data?.submission, "Submission data missing")
        val createdSubmissionId = response!!.data.submission.id
        assertNotNull(createdSubmissionId, "Submission ID should not be null")
        logger.info("Submitted task with submission: $createdSubmissionId")
        return createdSubmissionId
    }

    fun approveTask(taskId: IdType, token: String) {
        // Assuming PatchTaskRequestDTO exists
        val requestDTO = PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED) // Use enum DTO

        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>() // Expect task response DTO
            .value { response ->
                assertNotNull(response.data.task, "Task data missing")
                assertEquals(ApproveTypeDTO.APPROVED, response.data.task.approved)
            }
    }

    @BeforeAll
    fun prepare() {
        // Create users
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        participant = userCreatorService.createUser()
        participantToken = userCreatorService.login(participant.username, participant.password)

        // Create space
        val spaceResult =
            createSpace(
                creatorToken = creatorToken,
                spaceName = spaceName,
                spaceIntro = "This is a test space.",
                spaceDescription = "A lengthy text. ".repeat(100),
                spaceAvatarId = userCreatorService.testAvatarId(),
            )
        spaceId = spaceResult.first
        categoryId = spaceResult.second

        // Create attachment
        attachmentId = attachmentCreatorService.createAttachment(creatorToken)

        // Create task within the space
        taskId =
            createTask(
                taskName,
                TaskSubmitterType.USER,
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                spaceId,
                categoryId, // Use the default category ID from created space
            )

        approveTask(taskId, creatorToken) // Approve the task

        // Join task and submit
        participantTaskMembershipId = joinTask(taskId, participant.userId, participantToken)
        approveTaskParticipant(
            creatorToken,
            taskId,
            participantTaskMembershipId,
        ) // Approve participant
        submissionId =
            submitTask(
                taskId,
                participantTaskMembershipId,
                participantToken,
                attachmentId,
            ) // Submit
    }

    // --- Refactored Test Methods ---

    private fun assertReviewState(
        submission: TaskSubmissionDTO,
        expectedReviewed: Boolean,
        expectedAccepted: Boolean? = null,
        expectedScore: Int? = null,
        expectedComment: String? = null,
    ) {
        assertNotNull(submission.review, "Review object should exist")
        val review = submission.review!!
        assertEquals(expectedReviewed, review.reviewed, "Review 'reviewed' state mismatch")
        if (expectedReviewed) {
            assertNotNull(review.detail, "Review detail should exist when reviewed=true")
            val detail = review.detail!!
            assertEquals(expectedAccepted, detail.accepted, "Review 'accepted' state mismatch")
            assertEquals(expectedScore, detail.score, "Review 'score' mismatch")
            assertEquals(expectedComment, detail.comment, "Review 'comment' mismatch")
        } else {
            assertNull(review.detail, "Review detail should not exist when reviewed=false")
        }
    }

    @Test
    @Order(5)
    fun `test get submissions - queryReview=true - when not reviewed`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .build()
            }
            .header(
                "Authorization",
                "Bearer $participantToken",
            ) // Participant gets their submission
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                assertReviewState(submissions[0], expectedReviewed = false)
            }
    }

    @Test
    @Order(6)
    fun `test get submissions - queryReview=true, reviewed=true - when not reviewed`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .queryParam("reviewed", "true") // Filter for reviewed
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                assertTrue(
                    response.data.submissions.isEmpty(),
                    "Should find no reviewed submissions yet",
                )
            }
    }

    @Test
    @Order(7)
    fun `test get submissions - queryReview=true, reviewed=false - when not reviewed`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .queryParam("reviewed", "false") // Filter for not reviewed
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                assertReviewState(submissions[0], expectedReviewed = false)
            }
    }

    @Test
    @Order(10)
    fun `test create review - success`() {
        // Assuming PostTaskSubmissionReviewRequestDTO exists
        val requestDTO =
            PostTaskSubmissionReviewRequestDTO(accepted = true, score = 5, comment = "Good job!")

        webTestClient
            .post()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken") // Creator reviews
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<
                PostTaskSubmissionReview200ResponseDTO
            >() // Expect the specific DTO for review creation
            .value { response ->
                assertNotNull(response.data.submission)
                assertReviewState(
                    response.data.submission,
                    expectedReviewed = true,
                    expectedAccepted = true,
                    expectedScore = 5,
                    expectedComment = "Good job!",
                )
                // Optionally assert hasUpgradedParticipantRank if it's in the DTO
                // assertNotNull(response.data.hasUpgradedParticipantRank)
            }
    }

    @Test
    @Order(20)
    fun `test create review - forbidden for participant`() { // Renamed
        val requestDTO =
            PostTaskSubmissionReviewRequestDTO(accepted = true, score = 5, comment = "Good job!")

        webTestClient
            .post()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $participantToken") // Participant tries to review
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>() // Expect error DTO
            .value { errorResponse ->
                // Adjust expected error name if different
                assertEquals("AccessDeniedError", errorResponse.error.name)
            }
    }

    @Test
    @Order(30)
    fun `test create review - conflict when already reviewed`() { // Renamed
        val requestDTO =
            PostTaskSubmissionReviewRequestDTO(accepted = true, score = 5, comment = "Good job!")

        webTestClient
            .post()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken") // Creator tries again
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT) // 409 Conflict
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("TaskSubmissionAlreadyReviewedError", errorResponse.error.name)
            }
    }

    @Test
    @Order(40)
    fun `test update review - empty request does nothing`() { // Renamed
        val requestBody = emptyMap<String, Any?>() // Empty patch

        webTestClient
            .patch()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmissionReview200ResponseDTO>() // Assuming PATCH returns this DTO
            .value { response ->
                assertNotNull(response.data.submission)
                // Verify review state remains unchanged from test 10
                assertReviewState(
                    response.data.submission,
                    expectedReviewed = true,
                    expectedAccepted = true,
                    expectedScore = 5,
                    expectedComment = "Good job!",
                )
            }
    }

    @Test
    @Order(50)
    fun `test update review - success`() {
        // Assuming PatchTaskSubmissionReviewRequestDTO exists
        val requestDTO =
            PatchTaskSubmissionReviewRequestDTO(
                accepted = false,
                score = 4,
                comment = "Could be better.",
            )

        webTestClient
            .patch()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmissionReview200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submission)
                // Verify review state is updated
                assertReviewState(
                    response.data.submission,
                    expectedReviewed = true,
                    expectedAccepted = false,
                    expectedScore = 4,
                    expectedComment = "Could be better.",
                )
            }
    }

    @Test
    @Order(60)
    fun `test update review - forbidden for participant`() { // Renamed
        val requestDTO =
            PatchTaskSubmissionReviewRequestDTO(
                accepted = false,
                score = 4,
                comment = "Could be better.",
            )

        webTestClient
            .patch()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $participantToken") // Participant tries to update
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse -> assertEquals("AccessDeniedError", errorResponse.error.name) }
    }

    @Test
    @Order(70)
    fun `test get submissions - queryReview=true - after update`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .build()
            }
            .header(
                "Authorization",
                "Bearer $participantToken",
            ) // Participant gets their submission
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                // Verify state matches the update from test 50
                assertReviewState(
                    submissions[0],
                    expectedReviewed = true,
                    expectedAccepted = false,
                    expectedScore = 4,
                    expectedComment = "Could be better.",
                )
            }
    }

    @Test
    @Order(71)
    fun `test get submissions - queryReview=true, reviewed=true - after update`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .queryParam("reviewed", "true") // Filter for reviewed
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                // Verify state matches the update from test 50
                assertReviewState(
                    submissions[0],
                    expectedReviewed = true,
                    expectedAccepted = false,
                    expectedScore = 4,
                    expectedComment = "Could be better.",
                )
            }
    }

    @Test
    @Order(72)
    fun `test get submissions - queryReview=true, reviewed=false - after update`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .queryParam("reviewed", "false") // Filter for not reviewed
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                assertTrue(
                    response.data.submissions.isEmpty(),
                    "Should find no unreviewed submissions now",
                )
            }
    }

    @Test
    @Order(75)
    fun `test delete review - forbidden for participant`() { // Renamed
        webTestClient
            .delete()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $participantToken") // Participant tries to delete
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { errorResponse -> assertEquals("AccessDeniedError", errorResponse.error.name) }
    }

    @Test
    @Order(80)
    fun `test delete review - success`() {
        webTestClient
            .delete()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken") // Creator deletes
            .exchange()
            // Check status - Assuming 200 OK based on original test (though 204 No Content is
            // common)
            .expectStatus()
            .isOk
        // Body might be empty or contain a simple success message depending on API
        // If it returns a specific DTO, use .expectBody<DeleteReviewResponseDTO>()
        // If it returns Unit (no body), use .expectBody().isEmpty()
    }

    @Test
    @Order(90)
    fun `test get submissions - queryReview=true - after review deleted`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                // Verify review state is back to not reviewed
                assertReviewState(submissions[0], expectedReviewed = false)
            }
    }

    @Test
    @Order(100)
    fun `test delete review - not found when already deleted`() { // Renamed
        webTestClient
            .delete()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken") // Creator tries again
            .exchange()
            .expectStatus()
            .isNotFound // 404 Not Found
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("TaskSubmissionNotReviewedYetError", errorResponse.error.name)
            }
    }

    @Test
    @Order(110)
    fun `test update review - not found when review deleted`() { // Renamed
        val requestDTO =
            PatchTaskSubmissionReviewRequestDTO(
                accepted = false,
                score = 4,
                comment = "Could be better.",
            )

        webTestClient
            .patch()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isNotFound // 404 Not Found
            .expectBody<GenericErrorResponse>()
            .value { errorResponse ->
                assertEquals("TaskSubmissionNotReviewedYetError", errorResponse.error.name)
            }
    }

    @Test
    @Order(130)
    fun `test create review - success again after deletion`() { // Renamed
        val requestDTO =
            PostTaskSubmissionReviewRequestDTO(accepted = true, score = 5, comment = "Good job!")

        webTestClient
            .post()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PostTaskSubmissionReview200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submission)
                assertReviewState(
                    response.data.submission,
                    expectedReviewed = true,
                    expectedAccepted = true,
                    expectedScore = 5,
                    expectedComment = "Good job!",
                )
            }
    }

    @Test
    @Order(140)
    fun `test get submissions - queryReview=true - after recreation`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                assertReviewState(
                    submissions[0],
                    expectedReviewed = true,
                    expectedAccepted = true,
                    expectedScore = 5,
                    expectedComment = "Good job!",
                )
            }
    }

    @Test
    @Order(141)
    fun `test get submissions - queryReview=true, reviewed=true - after recreation`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .queryParam("reviewed", "true") // Filter reviewed
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                val submissions = response.data.submissions
                assertEquals(1, submissions.size)
                assertReviewState(
                    submissions[0],
                    expectedReviewed = true,
                    expectedAccepted = true,
                    expectedScore = 5,
                    expectedComment = "Good job!",
                )
            }
    }

    @Test
    @Order(142)
    fun `test get submissions - queryReview=true, reviewed=false - after recreation`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId/participants/$participantTaskMembershipId/submissions")
                    .queryParam("queryReview", "true")
                    .queryParam("reviewed", "false") // Filter not reviewed
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTaskSubmissions200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.submissions)
                assertTrue(
                    response.data.submissions.isEmpty(),
                    "Should be no unreviewed submissions",
                )
            }
    }
}
