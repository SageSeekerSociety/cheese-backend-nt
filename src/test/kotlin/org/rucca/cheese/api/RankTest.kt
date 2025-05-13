/*
 *  Description: It tests the feature of a user's rank in a space.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *      nameisyui
 *
 */

package org.rucca.cheese.api

import java.time.LocalDateTime
import kotlin.math.floor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.helper.toEpochMilli
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.GetSpaces200ResponseDTO
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class RankTest
@Autowired
constructor(
    private val webTestClient: WebTestClient, // Inject WebTestClient
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var participant: UserCreatorService.CreateUserResponse
    lateinit var participantToken: String

    // --- Test Data Initialization ---
    private val randomSuffix = floor(Math.random() * 10000000000).toLong()
    private var spaceName = "Test Space ($randomSuffix)"
    private var spaceIntro = "This is a test space."
    private var spaceDescription = "Description of space"
    private var spaceAvatarId = userCreatorService.testAvatarId()
    private var spaceId: IdType = -1
    private val taskName = "Test Task ($randomSuffix)"
    private val taskIntro = "This is a test task."
    private val taskDescription = "Description of task"
    private val taskDeadline = LocalDateTime.now().plusDays(7).toEpochMilli()
    // taskMembershipDeadline seems unused in prepare(), removed for now
    // private val taskMembershipDeadline = LocalDateTime.now().plusMonths(1).toEpochMilli()
    private val taskSubmissionSchema = listOf(Pair("Text Entry", "TEXT"))
    private var taskId: IdType = -1
    private var taskId2: IdType = -1
    private var taskId3: IdType = -1
    private var taskId4: IdType = -1 // Rank 2 Task
    private var submissionId: IdType = -1
    private var submissionId2: IdType = -1
    private var submissionId3: IdType = -1
    private var participantTaskMembershipId: IdType = -1
    private var participant2TaskMembershipId: IdType = -1
    private var participant3TaskMembershipId: IdType = -1

    // participant4TaskMembershipId seems unused in prepare(), removed for now
    // private var participant4TaskMembershipId: IdType = -1

    // --- Helper DTOs for Response Parsing ---
    private data class IdHolder(val id: IdType)

    private data class SpaceResponseData(val space: IdHolder)

    private data class SpaceResponse(val data: SpaceResponseData)

    private data class TaskResponseData(val task: IdHolder)

    private data class TaskResponse(val data: TaskResponseData)

    private data class ParticipantResponseData(
        val participant: IdHolder
    ) // Assuming API returns 'participant'

    private data class ParticipantResponse(val data: ParticipantResponseData)

    private data class SubmissionResponseData(val submission: IdHolder)

    private data class SubmissionResponse(val data: SubmissionResponseData)

    private data class TaskMembershipResponseData(
        val taskMembership: IdHolder
    ) // For approve response

    private data class TaskMembershipResponse(val data: TaskMembershipResponseData)

    // --- Refactored Helper Methods ---

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
    ): IdType {
        val requestBody =
            mapOf(
                "name" to spaceName,
                "intro" to spaceIntro,
                "description" to spaceDescription,
                "avatarId" to spaceAvatarId,
                "enableRank" to false, // Default from original test
                "announcements" to "[]", // Default from original test
                "taskTemplates" to "[]", // Default from original test
            )

        val response =
            webTestClient
                .post()
                .uri("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<SpaceResponse>() // Use type-safe DTO
                .returnResult()
                .responseBody

        requireNotNull(response) { "Response body was null after creating space" }
        val createdSpaceId = response.data.space.id
        logger.info("Created space: $createdSpaceId")
        return createdSpaceId
    }

    fun createTask(
        creatorToken: String,
        name: String,
        submitterType: String,
        deadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<Pair<String, String>>,
        team: IdType?,
        space: IdType?,
        rank: Int?,
    ): IdType {
        val schemaList = submissionSchema.map { mapOf("prompt" to it.first, "type" to it.second) }
        val requestBody =
            mutableMapOf<String, Any?>(
                    "name" to name,
                    "submitterType" to submitterType,
                    "deadline" to deadline, // Pass Long directly
                    "resubmittable" to resubmittable,
                    "editable" to editable,
                    "intro" to intro,
                    "description" to description,
                    "submissionSchema" to schemaList,
                    "team" to team, // Let framework handle null -> JSON null
                    "space" to space,
                    "rank" to rank,
                )
                .filterValues {
                    it != null
                } // Optionally remove nulls if API expects missing keys instead of null

        val response =
            webTestClient
                .post()
                .uri("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<TaskResponse>() // Use type-safe DTO
                .returnResult()
                .responseBody

        requireNotNull(response) { "Response body was null after creating task" }
        val createdTaskId = response.data.task.id
        logger.info("Created task: $createdTaskId")
        return createdTaskId
    }

    fun approveTask(taskId: IdType, token: String) {
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("approved" to "APPROVED"))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.task.approved")
            .isEqualTo("APPROVED")
    }

    fun addParticipantUser(token: String, taskId: IdType, userId: IdType): IdType {
        // Assuming the body is required even if simple, based on original test
        val requestBody = mapOf("email" to "test@example.com") // Example email

        val response =
            webTestClient
                .post()
                // Use uri builder for path variables and query params
                .uri { builder ->
                    builder.path("/tasks/$taskId/participants").queryParam("member", userId).build()
                }
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON) // Content type needed if body is sent
                .bodyValue(requestBody) // Sending the body
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<ParticipantResponse>() // Use type-safe DTO
                .returnResult()
                .responseBody

        requireNotNull(response) { "Response body was null after adding participant" }
        // Assuming the response key is 'participant' and contains an 'id'
        val participantId = response.data.participant.id
        logger.info("Added participant (membership) with ID: $participantId")
        return participantId
    }

    fun approveTaskParticipant(token: String, taskId: IdType, participantId: IdType) {
        webTestClient
            .patch()
            .uri("/tasks/$taskId/participants/$participantId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("approved" to "APPROVED"))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            // Verify correct path based on API response DTO structure
            .jsonPath("$.data.taskMembership.approved")
            .isEqualTo("APPROVED")
    }

    fun submitTaskUser(token: String, taskId: IdType, participantId: IdType): IdType {
        // Body is a JSON array of objects
        val requestBody = listOf(mapOf("text" to "This is a test submission."))

        val response =
            webTestClient
                .post()
                .uri("/tasks/$taskId/participants/$participantId/submissions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<SubmissionResponse>() // Use type-safe DTO
                .returnResult()
                .responseBody

        requireNotNull(response) { "Response body was null after submitting task" }
        val createdSubmissionId = response.data.submission.id
        logger.info("Created submission: $createdSubmissionId")
        return createdSubmissionId
    }

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        participant = userCreatorService.createUser()
        participantToken = userCreatorService.login(participant.username, participant.password)

        spaceId = createSpace(creatorToken, spaceName, spaceIntro, spaceDescription, spaceAvatarId)

        // Task 1 (Rank 1)
        taskId =
            createTask(
                creatorToken,
                taskName,
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                1,
            )
        approveTask(taskId, creatorToken)
        participantTaskMembershipId =
            addParticipantUser(participantToken, taskId, participant.userId)
        approveTaskParticipant(creatorToken, taskId, participantTaskMembershipId)
        submissionId = submitTaskUser(participantToken, taskId, participantTaskMembershipId)

        // Task 2 (Rank 2)
        taskId2 =
            createTask(
                creatorToken,
                taskName + " 2",
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                2,
            )
        approveTask(taskId2, creatorToken)
        participant2TaskMembershipId =
            addParticipantUser(participantToken, taskId2, participant.userId)
        approveTaskParticipant(creatorToken, taskId2, participant2TaskMembershipId)
        submissionId2 = submitTaskUser(participantToken, taskId2, participant2TaskMembershipId)

        // Task 3 (Rank 1, another one)
        taskId3 =
            createTask(
                creatorToken,
                taskName + " 3",
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                1,
            )
        approveTask(taskId3, creatorToken)
        participant3TaskMembershipId =
            addParticipantUser(participantToken, taskId3, participant.userId)
        approveTaskParticipant(creatorToken, taskId3, participant3TaskMembershipId)
        submissionId3 = submitTaskUser(participantToken, taskId3, participant3TaskMembershipId)

        // Task 4 (Rank 2, for join test later)
        taskId4 =
            createTask(
                creatorToken,
                taskName + " 4",
                "USER",
                taskDeadline,
                false,
                false,
                taskIntro,
                taskDescription,
                taskSubmissionSchema,
                null,
                spaceId,
                2,
            )
        approveTask(taskId4, creatorToken)
        // Participant does not join task 4 initially
    }

    // --- Refactored Test Methods ---

    @Test
    @Order(10)
    fun `test get space with myRank without rank enabled`() { // Renamed for clarity
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/spaces/$spaceId").queryParam("queryMyRank", "true").build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.id")
            .isEqualTo(spaceId)
            .jsonPath("$.data.space.myRank")
            .isEmpty // Check for absence or null
    }

    @Test
    @Order(20)
    fun `test enumerate spaces with myRank without rank enabled`() { // Renamed for clarity
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/spaces")
                    // Use camelCase parameter names matching controller/API definition if
                    // applicable
                    .queryParam(
                        "pageStart",
                        spaceId,
                    ) // Assuming pageStart is the correct param for pagination based on ID
                    .queryParam("queryMyRank", "true")
                    .queryParam("sortBy", "createdAt") // Default sort? Add if needed.
                    .queryParam("sortOrder", "asc") // Default sort? Add if needed.
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaces200ResponseDTO>()
            .value { response ->
                assertThat(response).isNotNull
                assertThat(response.data).isNotNull
                assertThat(response.data!!.spaces).isNotNull

                val targetSpace = response.data!!.spaces!!.find { it.id == spaceId }

                // Assert that the space was found
                assertThat(targetSpace)
                    .withFailMessage("Space with ID $spaceId not found in the response list")
                    .isNotNull

                // Assert properties of the found space
                assertThat(targetSpace!!.id)
                    .isEqualTo(spaceId) // Use !! because we asserted not null above
                assertThat(targetSpace.myRank).isNull() // Expecting rank to be null/absent
            }
    }

    @Test
    @Order(30)
    fun `test enable rank for space`() { // Renamed for clarity
        webTestClient
            .patch()
            .uri("/spaces/$spaceId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("enableRank" to true))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.enableRank")
            .isEqualTo(true)
    }

    @Test
    @Order(40)
    fun `test get space with myRank after enabling rank`() { // Renamed for clarity
        // Rank should be 0 initially after enabling, before any reviews that upgrade rank
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/spaces/$spaceId").queryParam("queryMyRank", "true").build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.id")
            .isEqualTo(spaceId)
            .jsonPath("$.data.space.myRank")
            .isEqualTo(0) // Expecting rank 0
    }

    @Test
    @Order(50)
    fun `test enumerate spaces with myRank after enabling rank`() { // Renamed for clarity
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/spaces")
                    .queryParam("pageStart", spaceId)
                    .queryParam("queryMyRank", "true")
                    .queryParam("sortBy", "createdAt")
                    .queryParam("sortOrder", "asc")
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetSpaces200ResponseDTO>()
            // Use .value() for assertions
            .value { response ->
                assertThat(response).isNotNull
                assertThat(response.data).isNotNull
                assertThat(response.data!!.spaces).isNotNull

                // Find the specific space
                val targetSpace = response.data!!.spaces!!.find { it.id == spaceId }

                // Assert it was found
                assertThat(targetSpace)
                    .withFailMessage("Space with ID $spaceId not found in the response list")
                    .isNotNull

                // Assert properties
                assertThat(targetSpace!!.id).isEqualTo(spaceId)
                assertThat(targetSpace.myRank).isEqualTo(0) // Expecting rank 0
            }
    }

    @Test
    @Order(55)
    fun `test join rank 2 task fails when rank is 0`() { // Renamed for clarity
        val requestBody = mapOf("email" to "test@example.com")

        webTestClient
            .post()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId4/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isBadRequest // Expecting error status
            .expectBody()
            .jsonPath("$.error.name")
            .isEqualTo("YourRankIsNotHighEnoughError") // Check error details
            .jsonPath("$.error.data.yourRank")
            .isEqualTo(0)
            .jsonPath("$.error.data.requiredRank")
            .isEqualTo(1) // Task rank 2 requires previous rank 1
    }

    @Test
    @Order(60)
    fun `test create failing review for rank 1 task submission`() { // Renamed for clarity
        val requestBody = mapOf("accepted" to false, "score" to 0, "comment" to "Holly shit!")
        webTestClient
            .post()
            .uri(
                "/tasks/$taskId/participants/$participantTaskMembershipId/submissions/$submissionId/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            // Check specific field indicating rank upgrade status
            .jsonPath("$.data.hasUpgradedParticipantRank")
            .isEqualTo(false)
    }

    @Test
    @Order(65)
    fun `test get space with myRank remains 0 after failing review`() { // Renamed for clarity
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/spaces/$spaceId").queryParam("queryMyRank", "true").build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.id")
            .isEqualTo(spaceId)
            .jsonPath("$.data.space.myRank")
            .isEqualTo(0) // Rank should not change
    }

    @Test
    @Order(70)
    fun `test update review for rank 1 task to accepted`() { // Renamed for clarity
        val requestBody =
            mapOf(
                "accepted" to true,
                "score" to
                    4, // Assuming score doesn't affect rank upgrade, only acceptance of rank 1 task
                "comment" to "Could be better.",
            )
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
            // Optionally check hasUpgradedParticipantRank if the PATCH returns it
            .expectBody()
            .jsonPath("$.data.hasUpgradedParticipantRank")
            .isEqualTo(true) // Rank should upgrade now
    }

    @Test
    @Order(75)
    fun `test join rank 2 task succeeds when rank is 1`() { // Renamed for clarity
        val requestBody = mapOf("email" to "test@example.com")

        webTestClient
            .post()
            .uri { builder ->
                builder
                    .path("/tasks/$taskId4/participants")
                    .queryParam("member", participant.userId)
                    .build()
            }
            .header("Authorization", "Bearer $participantToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk // Should succeed now
            // Optionally assert details of the created membership if needed
            .expectBody()
            .jsonPath("$.data.participant.id")
            .isNotEmpty // Check that a participant/membership was created
    }

    @Test
    @Order(80)
    fun `test get space with myRank is 1 after passing rank 1 task`() { // Renamed for clarity
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/spaces/$spaceId").queryParam("queryMyRank", "true").build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.id")
            .isEqualTo(spaceId)
            .jsonPath("$.data.space.myRank")
            .isEqualTo(1) // Rank should be 1
    }

    @Test
    @Order(90)
    fun `test create accepted review for rank 2 task submission`() { // Renamed for clarity
        val requestBody = mapOf("accepted" to true, "score" to 5, "comment" to "That's amazing!")
        webTestClient
            .post()
            .uri(
                "/tasks/$taskId2/participants/$participant2TaskMembershipId/submissions/$submissionId2/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.hasUpgradedParticipantRank")
            .isEqualTo(true) // Rank should upgrade
    }

    @Test
    @Order(100)
    fun `test get space with myRank is 2 after passing rank 2 task`() { // Renamed for clarity
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/spaces/$spaceId").queryParam("queryMyRank", "true").build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.id")
            .isEqualTo(spaceId)
            .jsonPath("$.data.space.myRank")
            .isEqualTo(2) // Rank should be 2
    }

    @Test
    @Order(110)
    fun `test create accepted review for another rank 1 task submission does not upgrade rank further`() { // Renamed for clarity
        val requestBody = mapOf("accepted" to true, "score" to 5, "comment" to "That's amazing!")
        webTestClient
            .post()
            .uri(
                "/tasks/$taskId3/participants/$participant3TaskMembershipId/submissions/$submissionId3/review"
            )
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.hasUpgradedParticipantRank")
            .isEqualTo(false) // Rank shouldn't upgrade again for a rank 1 task
    }

    @Test
    @Order(120)
    fun `test get space with myRank remains 2 after passing another rank 1 task`() { // Renamed for
        // clarity
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/spaces/$spaceId").queryParam("queryMyRank", "true").build()
            }
            .header("Authorization", "Bearer $participantToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.space.id")
            .isEqualTo(spaceId)
            .jsonPath("$.data.space.myRank")
            .isEqualTo(2) // Rank should remain 2
    }
}
