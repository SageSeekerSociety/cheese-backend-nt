/*
 *  Description: It tests the feature of project.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.CreateProjectRequestDTO
import org.rucca.cheese.utils.TeamCreatorService
import org.rucca.cheese.utils.UserCreatorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class ProjectTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
    private val teamCreatorService: TeamCreatorService,
) {
    lateinit var user: UserCreatorService.CreateUserResponse
    lateinit var userToken: String
    var teamId: IdType = -1

    @BeforeAll
    fun prepare() {
        user = userCreatorService.createUser()
        userToken = userCreatorService.login(user.username, user.password)
        teamId = teamCreatorService.createTeam(userToken)
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
    }

    @Test
    fun `test get projects`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/projects")
                    .header("Authorization", "Bearer $userToken")
                    .param("pageSize", "10")
                    .param("pageStart", "0")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.projects").isArray)
    }
}
