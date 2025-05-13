package org.rucca.cheese.api

import kotlin.math.floor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.CreateKnowledge200ResponseDTO
import org.rucca.cheese.model.KnowledgeGetById200ResponseDTO
import org.rucca.cheese.model.ListKnowledge200ResponseDTO
import org.rucca.cheese.model.UpdateKnowledge200ResponseDTO
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamMemberRole
import org.rucca.cheese.team.TeamRepository
import org.rucca.cheese.team.TeamUserRelation
import org.rucca.cheese.team.TeamUserRelationRepository
import org.rucca.cheese.user.AvatarRepository
import org.rucca.cheese.user.UserRepository
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
@TestMethodOrder(OrderAnnotation::class)
@TestInstance(Lifecycle.PER_CLASS)
class KnowledgeTest
@Autowired
constructor(
    private val webTestClient: WebTestClient,
    private val userCreatorService: UserCreatorService,
    private val teamRepository: TeamRepository,
    private val teamUserRelationRepository: TeamUserRelationRepository,
    private val userRepository: UserRepository,
    private val avatarRepository: AvatarRepository,
) {
    companion object {
        const val DEFAULT_AVATAR_ID = 1
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    // --- Shared state remains the same ---
    private lateinit var creatorToken: String
    private var knowledgeId: IdType = -1L // Use Long suffix
    private var teamId: IdType = -1L
    private var userId: IdType = -1L
    private val projectId: Long? = null // Keep as is from original
    private val labels = listOf("test", "demo")
    private val discussionId: Long? = null // Keep as is

    @BeforeAll
    fun setup() {
        logger.info("Starting KnowledgeIntegrationTest setup...")
        // Create user
        val creator = userCreatorService.createUser()
        userId = creator.userId
        creatorToken = userCreatorService.login(creator.username, creator.password)
        logger.info("User created (ID: {}) and logged in.", userId)

        // Create team (using repository directly is fine for setup)
        val avatar =
            avatarRepository.findById(DEFAULT_AVATAR_ID).orElseThrow {
                IllegalStateException("Default avatar with ID $DEFAULT_AVATAR_ID not found")
            }
        val team =
            Team(
                name = "Knowledge Test Team ${floor(Math.random() * 10000).toInt()}",
                intro = "Test team intro",
                description = "Test team description",
                avatar = avatar,
            )
        val savedTeam = teamRepository.save(team)
        teamId = requireNotNull(savedTeam.id) { "Saved team ID is null" }
        logger.info("Team created (ID: {})", teamId)

        // Add user to team
        val user =
            userRepository.findById(userId.toInt()).orElseThrow {
                IllegalStateException("User with ID $userId not found after creation")
            }
        val teamUserRelation =
            TeamUserRelation(
                user = user, // Pass the actual entity
                team = savedTeam,
                role = TeamMemberRole.OWNER, // Assuming this enum exists
            )
        teamUserRelationRepository.save(teamUserRelation)
        logger.info("User {} added to team {} as OWNER.", userId, teamId)
        logger.info("KnowledgeIntegrationTest setup complete.")
    }

    @Test
    @Order(1)
    fun `test create knowledge`() {
        val knowledgeName = "Test Knowledge ${floor(Math.random() * 1000000).toInt()}"
        val contentText = "This is a test knowledge content."
        val contentJsonString = Json.encodeToString(mapOf("text" to contentText))

        val requestBody =
            mapOf(
                "name" to knowledgeName,
                "description" to "A test knowledge description.",
                "type" to "TEXT", // Assuming type is a String, adjust if it's an Enum DTO
                "content" to contentJsonString,
                "teamId" to teamId,
                "projectId" to projectId, // Will be serialized as null if null
                "discussionId" to discussionId, // Will be serialized as null if null
                "labels" to labels,
            )

        // Use WebTestClient
        val response =
            webTestClient
                .post()
                .uri("/knowledge")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody)) // Send the map
                .exchange()
                .expectStatus()
                .isOk // Check status first
                .expectBody<CreateKnowledge200ResponseDTO>() // Expect specific DTO
                .returnResult()
                .responseBody // Get the deserialized DTO

        // Assertions on the DTO
        assertThat(response).isNotNull
        assertThat(response?.code).isEqualTo(200)
        val createdKnowledge = response?.data?.knowledge
        assertThat(createdKnowledge).isNotNull
        assertThat(createdKnowledge?.id).isGreaterThan(0L)
        assertThat(createdKnowledge?.name).isEqualTo(knowledgeName)
        assertThat(createdKnowledge?.content).isEqualTo(contentJsonString)
        assertThat(createdKnowledge?.teamId).isEqualTo(teamId)
        assertThat(createdKnowledge?.labels).containsExactlyInAnyOrderElementsOf(labels)

        // Capture the ID for subsequent tests
        knowledgeId = createdKnowledge?.id!! // Use !! only after checking isNotNull
        logger.info("Knowledge created with ID: {}", knowledgeId)
        assertThat(knowledgeId).isNotEqualTo(-1L)
    }

    @Test
    @Order(2)
    fun `test get knowledge success`() {
        Assumptions.assumeTrue(knowledgeId != -1L, "Knowledge ID must be set from previous test")
        logger.info("Getting knowledge with ID: {}", knowledgeId)

        webTestClient
            .get()
            .uri("/knowledge/$knowledgeId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<KnowledgeGetById200ResponseDTO>() // Use specific DTO
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                val knowledgeData = response.data
                assertThat(knowledgeData.id).isEqualTo(knowledgeId)
                assertThat(knowledgeData.teamId).isEqualTo(teamId)
                // Add more assertions on other fields if needed
                assertThat(knowledgeData.creator?.id)
                    .isEqualTo(userId) // Example check on nested DTO
            }
    }

    @Test
    @Order(3)
    fun `test get knowledge not found`() {
        webTestClient
            .get()
            .uri("/knowledge/99999999") // Use a clearly non-existent ID
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound // Check for 404
    }

    @Test
    @Order(4)
    fun `test get knowledges by team`() {
        webTestClient
            .get()
            .uri { builder -> // Use UriBuilder for query params
                builder
                    .path("/knowledge")
                    .queryParam("teamId", teamId.toString())
                    // Add other params like page, size if applicable
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListKnowledge200ResponseDTO>() // Expect list response DTO
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                val knowledgeList = response.data.knowledges
                assertThat(knowledgeList).isNotNull // Check if list exists
                assertThat(knowledgeList).isNotEmpty // Should have at least the one created
                // Find the knowledge created in this test run
                val createdKnowledge = knowledgeList!!.find { it.id == knowledgeId }
                assertThat(createdKnowledge).isNotNull
                assertThat(createdKnowledge?.teamId).isEqualTo(teamId)
                // Check pagination details if applicable
                // assertThat(response.data.page?.pageNumber).isEqualTo(0)
            }
    }

    @Test
    @Order(5)
    fun `test update knowledge success`() {
        Assumptions.assumeTrue(knowledgeId != -1L, "Knowledge ID must be set")

        val updatedName = "Updated Knowledge Name"
        val updatedDescription = "Updated knowledge description."
        val updatedContentText = "Updated content via integration test."
        val updatedContentJson = Json.encodeToString(mapOf("text" to updatedContentText))
        val updatedLabels = listOf("updated", "test-webclient")

        // Include only fields allowed by the PATCH endpoint spec
        // Omit fields that shouldn't be changed (like teamId, createdBy etc.)
        val updateRequest =
            mapOf(
                "name" to updatedName,
                "description" to updatedDescription,
                "content" to updatedContentJson,
                "labels" to updatedLabels,
                // "projectId" can be included if it's allowed to change
            )

        webTestClient
            .patch() // Assuming PATCH for updates
            .uri("/knowledge/$knowledgeId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(updateRequest))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<UpdateKnowledge200ResponseDTO>() // Expect update response DTO
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                val updatedKnowledge = response.data.knowledge
                assertThat(updatedKnowledge.id).isEqualTo(knowledgeId)
                assertThat(updatedKnowledge.name).isEqualTo(updatedName)
                assertThat(updatedKnowledge.description).isEqualTo(updatedDescription)
                assertThat(updatedKnowledge.content).isEqualTo(updatedContentJson)
                assertThat(updatedKnowledge.teamId).isEqualTo(teamId) // Verify teamId didn't change
                assertThat(updatedKnowledge.labels)
                    .containsExactlyInAnyOrderElementsOf(updatedLabels)
                // Verify 'updatedAt' timestamp is recent (more complex assertion)
                assertThat(updatedKnowledge.updatedAt)
                    .withFailMessage(
                        "Expected updatedAt (%s) to be >= createdAt (%s)",
                        updatedKnowledge.updatedAt,
                        updatedKnowledge.createdAt,
                    )
                    .isGreaterThanOrEqualTo(updatedKnowledge.createdAt)
            }
    }

    @Test
    @Order(6)
    fun `test delete knowledge success`() {
        Assumptions.assumeTrue(knowledgeId != -1L, "Knowledge ID must be set")
        logger.info("Attempting to delete knowledge ID: {}", knowledgeId)

        webTestClient
            .delete()
            .uri("/knowledge/$knowledgeId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .exchange()
            // Check the expected status code for successful deletion
            // It might be 200 OK (with optional body) or 204 No Content
            .expectStatus()
            .isOk // Adjust to .isNoContent() if your API returns 204
        // If expecting 200 OK with a body:
        // .expectBody<DeleteKnowledgeResponseDTO>()
        // .value { response ->
        //      assertThat(response.code).isEqualTo(200) // Or specific success code
        // }
        // If expecting 204 No Content, don't use expectBody()

        // Optional: Verify deletion by trying to GET it again
        webTestClient
            .get()
            .uri("/knowledge/$knowledgeId")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound // Should now be 404

        // Mark as deleted for cleanup phase if needed (though it's already deleted)
        // knowledgeId = -1L // Or use a flag
        logger.info(
            "Knowledge ID {} deleted successfully (verified by subsequent 404).",
            knowledgeId,
        )
        // Setting ID back to -1 prevents trying to delete it again in @AfterAll
        knowledgeId = -1L
    }
    //
    //    @AfterAll
    //    fun cleanup() {
    //        logger.info("Starting KnowledgeIntegrationTest cleanup...")
    //        // Use try-catch to ensure cleanup attempts happen even if one fails
    //        try {
    //            // 1. Delete Knowledge (if not already deleted in test and ID is valid)
    //            // The delete test already sets knowledgeId to -1L upon successful verification
    //            if (knowledgeId != -1L) {
    //                logger.warn("Knowledge ID {} was not deleted during tests, attempting
    // cleanup.", knowledgeId)
    //                webTestClient.delete()
    //                    .uri("/knowledge/$knowledgeId")
    //                    .header(HttpHeaders.AUTHORIZATION, "Bearer $creatorToken")
    //                    .exchange()
    //                    .expectStatus().isOk // Or isNoContent or isNotFound (if already gone)
    //                logger.info("Attempted cleanup deletion for knowledge ID: {}", knowledgeId)
    //            }
    //
    //            // 2. Delete Team User Relation (using repository is simplest)
    //            teamUserRelationRepository.findByTeamIdAndUserId(teamId, userId).ifPresent {
    //                teamUserRelationRepository.delete(it)
    //                logger.info("Deleted team relation for user {} in team {}", userId, teamId)
    //            }
    //
    //            // 3. Delete Team (using repository)
    //            if (teamId != -1L) {
    //                teamRepository.deleteById(teamId)
    //                logger.info("Deleted team ID: {}", teamId)
    //            }
    //
    //            // 4. Delete User (using service)
    //            userCreatorService.deleteUser(userId) // Assumes this method exists and works
    //            logger.info("Deleted user ID: {}", userId)
    //
    //        } catch (e: Exception) {
    //            logger.error("Error during KnowledgeIntegrationTest cleanup: {}", e.message, e)
    //        }
    //        logger.info("KnowledgeIntegrationTest cleanup finished.")
    //    }
}
