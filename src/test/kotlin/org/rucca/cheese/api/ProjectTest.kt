/*
 *  Description: It tests the feature of project.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

// Keep necessary imports from the original code if they are still used
// Import necessary DTOs for response validation if needed, e.g.:
// import org.rucca.cheese.model.CreateProject201ResponseDTO
// import org.rucca.cheese.model.GetProjects200ResponseDTO
import kotlin.math.floor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.CreateProjectRequestDTO // Assuming this DTO is still correct
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient // Import WebTestClient
import org.springframework.test.web.reactive.server.expectBody // For expectBody extensions

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
) // Use WebTestClient environment
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @AutoConfigureMockMvc // Remove this annotation
@TestMethodOrder(OrderAnnotation::class)
class ProjectTest
@Autowired
constructor(
    private val webTestClient: WebTestClient, // Inject WebTestClient
    private val userCreatorService: UserCreatorService,
    // private val objectMapper: ObjectMapper // Usually not needed directly with WebTestClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var user: UserCreatorService.CreateUserResponse
    lateinit var userToken: String
    var teamId: IdType = -1

    // Define simple helper DTOs for createTeam response parsing for type safety
    private data class TeamIdHolder(val id: IdType)

    private data class CreateTeamResponseData(val team: TeamIdHolder)

    private data class CreateTeamResponse(val data: CreateTeamResponseData)

    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val requestBody =
            mapOf(
                "name" to teamName,
                "intro" to teamIntro,
                "description" to teamDescription,
                "avatarId" to teamAvatarId,
            )

        val response =
            webTestClient
                .post()
                .uri("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody) // Use bodyValue for simple maps/objects
                .exchange()
                .expectStatus()
                .isOk // Check HTTP status first
                .expectBody<CreateTeamResponse>() // Expect and deserialize the response body
                .returnResult()
                .responseBody

        requireNotNull(response) { "Response body was null after creating team" }
        val createdTeamId = response.data.team.id
        logger.info("Created team: $createdTeamId")
        return createdTeamId
    }

    @BeforeAll
    fun prepare() {
        user = userCreatorService.createUser()
        userToken = userCreatorService.login(user.username, user.password)
        teamId =
            createTeam(
                userToken,
                teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})",
                teamIntro = "This is a test team.",
                teamDescription = "A lengthy text. ".repeat(1000),
                teamAvatarId = userCreatorService.testAvatarId(),
            )
    }

    @Test
    fun `test create project`() {
        val request =
            CreateProjectRequestDTO(
                name = "Test Project",
                description = "Test Description",
                colorCode = "#FFFFFF",
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + 86400000, // 1 day later
                teamId = teamId,
                leaderId = user.userId,
                content = "Test Content", // Assuming 'content' was missing but intended
                parentId = null,
                externalTaskId = null,
                githubRepo = null,
            )

        webTestClient
            .post()
            .uri("/projects")
            .header("Authorization", "Bearer $userToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request) // Automatically serializes the DTO
            .exchange() // Execute the request
            .expectStatus()
            .isOk // Assert HTTP status
            // Assert response body content using JSONPath
            .expectBody()
            .jsonPath("$.data.project.name")
            .isEqualTo("Test Project")
            // Optionally: Deserialize to the full response DTO for more comprehensive checks
            // .expectBody(CreateProject201ResponseDTO::class.java)
            // .value { response -> assertEquals("Test Project", response.data.project.name) }
            .consumeWith {
                logger.info("Create project response: $it")
            } // Log response details if needed
    }

    @Test
    fun `test get projects`() {
        webTestClient
            .get()
            // Build URI with query parameters correctly
            .uri { uriBuilder ->
                uriBuilder
                    .path("/projects")
                    .queryParam("team_id", teamId) // Use the expected query param name
                    .build()
            }
            .header("Authorization", "Bearer $userToken")
            .exchange() // Execute the request
            .expectStatus()
            .isOk // Assert HTTP status
            // Assert response body structure using JSONPath
            .expectBody()
            .jsonPath("$.data.projects")
            .isArray // Check if 'projects' is an array
            // Optionally: Deserialize to the full response DTO
            // .expectBody(GetProjects200ResponseDTO::class.java)
            // .value { response -> assertTrue(response.data.projects.isNotEmpty()) } // Example
            // check
            .consumeWith {
                logger.info("Get projects response: $it")
            } // Log response details if needed
    }
}
