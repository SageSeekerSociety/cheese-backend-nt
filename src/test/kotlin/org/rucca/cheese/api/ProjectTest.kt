/*
 *  Description: It tests the feature of project.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.CreateProjectRequestDTO
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class ProjectTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {

    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var user: UserCreatorService.CreateUserResponse
    lateinit var userToken: String
    var teamId: IdType = -1

    fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$teamName",
                  "intro": "$teamIntro",
                  "description": "$teamDescription",
                  "avatarId": $teamAvatarId
                }
            """
                )
        val teamId =
            JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("team")
                .getLong("id")
        logger.info("Created team: $teamId")
        return teamId
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
                endDate = System.currentTimeMillis() + 86400000,
                teamId = teamId,
                leaderId = user.userId,
                content = "Test Content",
                parentId = null,
                externalTaskId = null,
                githubRepo = null,
            )

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/projects")
                    .header("Authorization", "Bearer $userToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ObjectMapper().writeValueAsString(request))
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.project.name").value("Test Project"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `test get projects`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/projects")
                    .header("Authorization", "Bearer $userToken")
                    .param("team_id", teamId.toString())
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.projects").isArray)
            .andDo(MockMvcResultHandlers.print())
    }
}
