package org.rucca.cheese.api

// Removed: import org.json.JSONObject // Use ObjectMapper instead
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.floor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.discussion.ReactionType
import org.rucca.cheese.discussion.ReactionTypeRepository
import org.rucca.cheese.model.*
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class DiscussionTest {
    @Autowired private lateinit var webTestClient: WebTestClient
    @Autowired
    private lateinit var objectMapper: ObjectMapper // For serializing request bodies if needed

    // Keep dependencies needed for data setup
    @Autowired private lateinit var userCreatorService: UserCreatorService
    @Autowired private lateinit var reactionTypeRepository: ReactionTypeRepository
    // Removed @MockkBeans for services, jwt, permissionEvaluator

    private val logger = LoggerFactory.getLogger(javaClass)

    // --- Shared state for tests ---
    private lateinit var creator: UserCreatorService.CreateUserResponse
    private lateinit var creatorToken: String
    private var teamId: IdType = -1
    private var projectId: IdType = -1
    private var createdDiscussionId: Long = 0 // Will be set in testCreateDiscussion
    private var reactionTypeId: IdType = -1

    // --- Helper Functions modified to use WebTestClient ---

    /**
     * Creates a team via API call using WebTestClient. This is now part of the integration test
     * setup.
     */
    private fun createTeamViaApi(
        token: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val teamPayload =
            mapOf(
                "name" to teamName,
                "intro" to teamIntro,
                "description" to teamDescription,
                "avatarId" to teamAvatarId,
            )

        val responseBody =
            webTestClient
                .post()
                .uri("/teams")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(teamPayload))
                .exchange()
                .expectStatus()
                .isOk // Or isCreated if that's what your API returns
                // Use expectBody to parse the response and extract the ID
                .expectBody<Map<String, Any>>() // Expect a Map structure based on previous jsonPath
                .returnResult()
                .responseBody

        // Navigate the map safely - adjust paths based on your actual Team creation response DTO
        val teamData = responseBody?.get("data") as? Map<*, *>
        val team = teamData?.get("team") as? Map<*, *>
        val id = team?.get("id") as? Number // Use Number for flexibility (Int or Long)

        requireNotNull(id) { "Could not parse team ID from response: $responseBody" }

        val extractedId = id.toLong()
        logger.info("Created team via API with ID: {}", extractedId)
        return extractedId
    }

    /**
     * Creates a project via API call using WebTestClient. This is now part of the integration test
     * setup.
     */
    private fun createProjectViaApi(token: String, teamId: IdType, creatorId: IdType): IdType {
        // Use the actual DTO expected by the /projects endpoint
        val requestDTO =
            CreateProjectRequestDTO(
                name = "Test Project (${floor(Math.random() * 10000000000).toLong()})",
                description = "Test Description",
                colorCode = "#FFFFFF",
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + 86400000, // 1 day later
                teamId = teamId,
                leaderId = creatorId,
                content = "Test Project Content",
                parentId = null,
                externalTaskId = null,
                githubRepo = null,
            )

        val responseBody =
            webTestClient
                .post()
                .uri("/projects")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestDTO)) // Pass the DTO directly
                .exchange()
                .expectStatus()
                .isOk // Or isCreated
                .expectBody<Map<String, Any>>() // Adjust type based on actual response DTO
                .returnResult()
                .responseBody

        // Navigate the map safely - adjust paths based on your actual Project creation response DTO
        val projectData = responseBody?.get("data") as? Map<*, *>
        val project = projectData?.get("project") as? Map<*, *>
        val id = project?.get("id") as? Number

        requireNotNull(id) { "Could not parse project ID from response: $responseBody" }

        val extractedId = id.toLong()
        logger.info("Created project via API with ID: {}", extractedId)
        return extractedId
    }

    // --- Test Setup (runs once for the class) ---
    @BeforeAll
    fun prepareTestData() {
        // Create user using the service (or API if you have one)
        creator = userCreatorService.createUser()
        // Login using the service (or API) to get a real token
        creatorToken = userCreatorService.login(creator.username, creator.password)
        logger.info("Test user created and logged in. User ID: ${creator.userId}, Token obtained.")

        // Create team using the API via WebTestClient
        try {
            teamId =
                createTeamViaApi(
                    creatorToken,
                    teamName =
                        "Integration Test Team (${floor(Math.random() * 10000000000).toLong()})",
                    teamIntro = "This is an integration test team.",
                    teamDescription = "A lengthy text description. ".repeat(20),
                    teamAvatarId =
                        userCreatorService.testAvatarId(), // Assuming this gives a valid ID
                )
        } catch (e: Exception) {
            logger.error("Failed to create team during API setup", e)
            throw IllegalStateException("Failed to create team in @BeforeAll via API", e)
        }

        // Create project using the API via WebTestClient
        try {
            projectId = createProjectViaApi(creatorToken, teamId, creator.userId)
        } catch (e: Exception) {
            logger.error("Failed to create project during API setup", e)
            throw IllegalStateException("Failed to create project in @BeforeAll via API", e)
        }

        // Ensure reaction type exists in DB (using repository directly is fine for setup)
        val reactionCode = "thumbs_up"
        var reactionType = reactionTypeRepository.findByCode(reactionCode)
        if (reactionType == null) {
            reactionType =
                ReactionType(
                    code = reactionCode,
                    name = "👍 Thumbs Up",
                    description = "A thumbs up reaction",
                    displayOrder = 1,
                    isActive = true,
                )
            reactionType = reactionTypeRepository.save(reactionType)
            logger.info("Saved reaction type with code: {}", reactionCode)
        }

        reactionTypeId =
            reactionType.id
                ?: throw IllegalStateException("Failed to get ID for ReactionType '$reactionCode'")
        logger.info("Using reaction type ID for tests: {}", reactionTypeId)
        logger.info("Test data setup complete. TeamID: {}, ProjectID: {}", teamId, projectId)
    }

    // --- Test Cases using WebTestClient and real services ---

    @Test
    @Order(1)
    fun `GET discussions endpoint returns discussion list`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/discussions")
                    .queryParam("modelType", "PROJECT")
                    .queryParam("modelId", projectId) // Use the real projectId from setup
                    .queryParam("pageStart", "0") // Example query params
                    .queryParam("pageSize", "10")
                    .queryParam("sortBy", "createdAt")
                    .queryParam("sortOrder", "desc")
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken") // Use real token
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            // Expect the specific Response DTO for type safety
            .expectBody<ListDiscussions200ResponseDTO>()
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.data).isNotNull
                assertThat(response.data.discussions)
                    .isNotNull // Check list exists (might be empty initially)
                assertThat(response.data.page).isNotNull
                // Add more specific assertions if needed, e.g., check page details
                assertThat(response.data.page?.pageStart)
                    .isEqualTo(0L) // Assuming PageDTO uses Long
            }
    }

    @Test
    @Order(2)
    fun `POST discussions endpoint creates a new discussion`() {
        // Prepare content and request DTO
        val contentObj =
            mapOf(
                "type" to "doc",
                "content" to
                    listOf(
                        mapOf(
                            "type" to "paragraph",
                            "content" to
                                listOf(
                                    mapOf("type" to "text", "text" to "Integration test content")
                                ),
                        )
                    ),
            )
        // Serialize content map to JSON string *if* CreateDiscussionRequestDTO.content expects
        // String
        val contentJsonString = objectMapper.writeValueAsString(contentObj)

        val requestDto =
            CreateDiscussionRequestDTO(
                content = contentJsonString, // Pass the serialized string
                parentId = null,
                mentionedUserIds = emptyList(),
                modelType = DiscussableModelTypeDTO.PROJECT, // Use DTO Enum
                modelId = projectId, // Use real projectId
            )

        val response =
            webTestClient
                .post()
                .uri("/discussions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestDto)) // Send the DTO
                .exchange()
                .expectStatus()
                .isOk // Or isCreated if applicable
                .expectBody<CreateDiscussion200ResponseDTO>() // Expect specific response DTO
                .returnResult()
                .responseBody

        // Assertions on the response DTO
        assertThat(response).isNotNull
        assertThat(response?.code).isEqualTo(200)
        val discussionData = response?.data?.discussion
        assertThat(discussionData).isNotNull
        assertThat(discussionData?.id).isGreaterThan(0L) // Check ID is generated
        assertThat(discussionData?.content).isEqualTo(contentJsonString) // Check content saved
        assertThat(discussionData?.sender?.id).isEqualTo(creator.userId)
        assertThat(discussionData?.modelId).isEqualTo(projectId)
        assertThat(discussionData?.modelType).isEqualTo(DiscussableModelTypeDTO.PROJECT)

        // Capture the created discussion ID for the next test
        createdDiscussionId =
            discussionData?.id!! // Use !! only if confident it's not null based on previous asserts
        logger.info("Captured created discussion ID: {}", createdDiscussionId)
        Assertions.assertTrue(createdDiscussionId > 0, "Failed to capture created discussion ID")
    }

    @Test
    @Order(3)
    fun `POST discussion reaction endpoint adds a reaction`() {
        // Pre-conditions check (ensure previous test ran and set the ID)
        Assumptions.assumeTrue(
            createdDiscussionId > 0,
            "Discussion must be created in Order(2) test",
        )
        Assumptions.assumeTrue(reactionTypeId > 0, "Reaction Type ID must be valid")

        webTestClient
            .post()
            // Use the IDs captured/set during setup
            .uri("/discussions/$createdDiscussionId/reactions/$reactionTypeId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .accept(MediaType.APPLICATION_JSON)
            // No request body typically needed for this kind of toggle/add endpoint
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ReactToDiscussion200ResponseDTO>() // Expect specific DTO
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                val reactionData = response.data.reaction
                assertThat(reactionData).isNotNull
                assertThat(reactionData?.count).isGreaterThan(0L)
                assertThat(reactionData?.reactionType?.id).isEqualTo(reactionTypeId)
                assertThat(reactionData?.hasReacted).isEqualTo(true)
            }

        // Optionally: Verify the reaction exists by fetching the discussion again
        webTestClient
            .get()
            .uri("/discussions/$createdDiscussionId") // Assuming a GET by ID endpoint exists
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetDiscussion200ResponseDTO>()
            .value { response ->
                val discussion = response.data.discussion
                assertThat(discussion.reactions?.map { it.reactionType.id })
                    .contains(reactionTypeId)
                assertThat(
                        discussion.reactions?.find { it.reactionType.id == reactionTypeId }?.count
                    )
                    .isGreaterThanOrEqualTo(1)
            }
        logger.info("Verified reaction was added to discussion {}", createdDiscussionId)
    }

    //    // --- TODO: Add more integration tests ---
    //    // - Get specific discussion (/discussions/{id})
    //    // - Get sub-discussions (/discussions/{id}/sub-discussions)
    //    // - Patch discussion (/discussions/{id})
    //    // - Delete discussion (/discussions/{id}) - Careful with test order!
    //    // - Toggle reaction off (call POST reaction endpoint again)
    //    // - Get all reaction types (/reaction-types) - This one might not need auth
    //
    //    // --- Cleanup (runs once after all tests in the class) ---
    //    @AfterAll
    //    fun cleanupTestData() {
    //        logger.info("Starting test data cleanup...")
    //        try {
    //            // Order is important: Delete dependent entities first
    //
    //            // 1. Delete reactions (if endpoint exists or use service/repo)
    //            // Example: Assuming a DELETE endpoint or service method exists
    //            // reactionRepository.deleteByDiscussionId(createdDiscussionId) // Or similar
    //
    //            // 2. Delete the created discussion (if ID was captured)
    //            if (createdDiscussionId > 0) {
    //                webTestClient.delete()
    //                    .uri("/discussions/$createdDiscussionId")
    //                    .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
    //                    .exchange()
    //                    .expectStatus().isNoContent // Or isOk/isNotFound depending on your API
    //                logger.info("Deleted discussion with ID: {}", createdDiscussionId)
    //            }
    //
    //            // 3. Delete the project
    //            if (projectId > 0) {
    //                webTestClient.delete()
    //                    .uri("/projects/$projectId") // Assuming DELETE /projects/{id} exists
    //                    .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
    //                    .exchange()
    //                    .expectStatus().isNoContent // Or isOk/isNotFound
    //                logger.info("Deleted project with ID: {}", projectId)
    //            }
    //
    //            // 4. Delete the team
    //            if (teamId > 0) {
    //                webTestClient.delete()
    //                    .uri("/teams/$teamId") // Assuming DELETE /teams/{id} exists
    //                    .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
    //                    .exchange()
    //                    .expectStatus().isNoContent // Or isOk/isNotFound
    //                logger.info("Deleted team with ID: {}", teamId)
    //            }
    //
    //            // 5. Delete the user (using service is often easiest for cleanup)
    //            userCreatorService.deleteUser(creator.userId)
    //            logger.info("Deleted user with ID: {}", creator.userId)
    //
    //        } catch (e: Exception) {
    //            logger.error("Error during test data cleanup", e)
    //            // Don't fail the build for cleanup errors, but log them
    //        }
    //        logger.info("Test data cleanup finished.")
    //    }
}
