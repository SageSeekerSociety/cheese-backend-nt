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
import org.slf4j.LoggerFactory
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
class SpaceTest
@Autowired
constructor(
        private val mockMvc: MockMvc,
        private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var user: UserCreatorService.CreateUserResponse
    lateinit var token: String
    private val spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    private val spaceIntro = "This is a test space."
    private val spaceAvatarId = userCreatorService.testAvatarId()
    var spaceId: IdType = -1

    @BeforeAll
    fun prepare() {
        user = userCreatorService.createUser()
        token = userCreatorService.login(user.username, user.password)
    }

    @Test
    @Order(10)
    fun testGetSpaceAndNotFound() {
        val request = MockMvcRequestBuilders.get("/spaces/-1").header("Authorization", "Bearer $token")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("NotFoundError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.type").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.id").value("-1"))
    }

    @Test
    @Order(20)
    fun testCreateSpace() {
        val request =
                MockMvcRequestBuilders.post("/spaces")
                        .header("Authorization", "Bearer $token")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "name": "$spaceName",
                    "intro": "$spaceIntro",
                    "avatarId": $spaceAvatarId
                }
            """)
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(user.userId))
        spaceId =
                JSONObject(response.andReturn().response.contentAsString)
                        .getJSONObject("data")
                        .getJSONObject("space")
                        .getLong("id")
        logger.info("Created space: $spaceId")
    }

    @Test
    @Order(30)
    fun testGetSpace() {
        val request = MockMvcRequestBuilders.get("/spaces/$spaceId").header("Authorization", "Bearer $token")
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(user.userId))
    }

    @Test
    @Order(40)
    fun testNameAlreadyExists() {
        val request =
                MockMvcRequestBuilders.post("/spaces")
                        .header("Authorization", "Bearer $token")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "name": "$spaceName",
                    "intro": "$spaceIntro",
                    "avatarId": $spaceAvatarId
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isConflict)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("NameAlreadyExistsError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.type").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.name").value(spaceName))
    }
}
