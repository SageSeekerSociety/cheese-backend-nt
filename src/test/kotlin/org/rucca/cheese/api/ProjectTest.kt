/*
 *  Description: It tests the feature of project.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.client.ProjectClient
import org.rucca.cheese.client.TeamClient
import org.rucca.cheese.client.UserClient
import org.rucca.cheese.common.persistent.IdType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class ProjectTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userClient: UserClient,
    private val teamClient: TeamClient,
    private val projectClient: ProjectClient,
) {
    lateinit var user: UserClient.CreateUserResponse
    lateinit var userToken: String
    var teamId: IdType = -1
    var projectId: IdType = -1
    val projectName = "Test Project"
    val projectDescription = "Test Description"
    val projectColorCode = "#FFFFFF"
    val projectStartDate: Long = System.currentTimeMillis()
    val projectEndDate: Long = System.currentTimeMillis() + 86400000
    val projectContent = "Test Content"

    @BeforeAll
    fun prepare() {
        user = userClient.createUser()
        userToken = userClient.login(user.username, user.password)
        teamId = teamClient.createTeam(userToken)
    }

    @Test
    @Order(10)
    fun `test create project`() {
        projectId =
            projectClient.createProject(
                userToken,
                name = projectName,
                description = projectDescription,
                colorCode = projectColorCode,
                startDate = projectStartDate,
                endDate = projectEndDate,
                content = projectContent,
                teamId = teamId,
                leaderId = user.userId,
            )
    }

    @Test
    @Order(20)
    fun `test get projects`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/projects")
                    .header("Authorization", "Bearer $userToken")
                    .param("pageSize", "10")
                    .param("pageStart", "0")
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projects").isArray)
    }

    @Test
    @Order(30)
    fun `test get project by id`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/projects/$projectId")
                    .header("Authorization", "Bearer $userToken")
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.project.id").value(projectId))
            .andExpect(jsonPath("$.data.project.name").value(projectName))
            .andExpect(jsonPath("$.data.project.description").value(projectDescription))
            .andExpect(jsonPath("$.data.project.colorCode").value(projectColorCode))
            .andExpect(jsonPath("$.data.project.startDate").value(projectStartDate))
            .andExpect(jsonPath("$.data.project.endDate").value(projectEndDate))
            .andExpect(jsonPath("$.data.project.content").value(projectContent))
    }
}
