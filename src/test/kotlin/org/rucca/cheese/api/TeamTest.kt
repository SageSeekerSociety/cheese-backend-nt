package org.rucca.cheese.api

import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.auth.UserCreatorService
import org.rucca.cheese.common.persistent.IdType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TeamTest
@Autowired
constructor(
        private val mockMvc: MockMvc,
        private val userCreatorService: UserCreatorService,
) {
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    private var teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})"
    private var teamIntro = "This is a test team"
    private var teamAvatarId = userCreatorService.testAvatarId()
    private var teamId: IdType = -1

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
    }

    @Test
    @Order(10)
    fun testCreateTeam() {
        val request =
                MockMvcRequestBuilders.post("/teams")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "name": "$teamName",
                  "intro": "$teamIntro",
                  "avatarId": $teamAvatarId
                }
            """)
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.name").value(teamName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.intro").value(teamIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.avatarId").value(teamAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(1))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[0].id")
                                        .value(creator.userId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(0))
        teamId =
                JSONObject(response.andReturn().response.contentAsString)
                        .getJSONObject("data")
                        .getJSONObject("team")
                        .getLong("id")
    }

    @Test
    @Order(20)
    fun testGetTeam() {
        val request = MockMvcRequestBuilders.get("/teams/$teamId").header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.name").value(teamName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.intro").value(teamIntro))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.avatarId").value(teamAvatarId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[0].id").value(creator.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(0))
    }
}
