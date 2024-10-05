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
class RankTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    private var spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    private var spaceIntro = "This is a test space."
    private var spaceAvatarId = userCreatorService.testAvatarId()
    private var spaceId: IdType = -1

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceAvatarId: IdType
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "name": "$spaceName",
                    "intro": "$spaceIntro",
                    "avatarId": $spaceAvatarId
                }
            """
                )
        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER")
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id")
                        .value(creator.userId)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.enableRank").value(false))
        val spaceId =
            JSONObject(response.andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("space")
                .getLong("id")
        logger.info("Created space: $spaceId")
        return spaceId
    }

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        spaceId = createSpace(creatorToken, spaceName, spaceIntro, spaceAvatarId)
    }

    @Test
    @Order(10)
    fun testGetSpaceWithMyRankWithoutRank() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").isEmpty)
    }

    @Test
    @Order(20)
    fun testEnumerateSpacesWithMyRankWithoutRank() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .param("page_start", spaceId.toString())
                .param("queryMyRank", "true")
        mockMvc
            .perform(requestBuilders)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].myRank").isEmpty)
    }

    @Test
    @Order(30)
    fun testPatchSpaceWithFullRequest() {
        val request =
            MockMvcRequestBuilders.patch("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("""{ "enableRank": true }""")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.enableRank").value(true))
    }

    @Test
    @Order(40)
    fun testGetSpaceWithMyRank() {
        val request =
            MockMvcRequestBuilders.get("/spaces/$spaceId")
                .header("Authorization", "Bearer $creatorToken")
                .param("queryMyRank", "true")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.myRank").value(0))
    }

    @Test
    @Order(50)
    fun testEnumerateSpacesWithMyRank() {
        val requestBuilders =
            MockMvcRequestBuilders.get("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .param("page_start", spaceId.toString())
                .param("queryMyRank", "true")
        mockMvc
            .perform(requestBuilders)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].id").value(spaceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].myRank").value(0))
    }
}
