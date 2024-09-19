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
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var admin: UserCreatorService.CreateUserResponse
    lateinit var adminToken: String
    lateinit var newOwner: UserCreatorService.CreateUserResponse
    lateinit var newOwnerToken: String
    lateinit var anonymous: UserCreatorService.CreateUserResponse
    lateinit var anonymousToken: String
    private var spaceName = "Test Space (${floor(Math.random() * 10000000000).toLong()})"
    private var originalSpaceName = spaceName
    private var spaceIntro = "This is a test space."
    private var spaceAvatarId = userCreatorService.testAvatarId()
    private var spaceId: IdType = -1
    private var spaceIdOfSecond: IdType = -1
    private var spaceIdOfBeforeLast: IdType = -1
    private var spaceIdOfLast: IdType = -1

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        admin = userCreatorService.createUser()
        adminToken = userCreatorService.login(admin.username, admin.password)
        newOwner = userCreatorService.createUser()
        newOwnerToken = userCreatorService.login(newOwner.username, newOwner.password)
        anonymous = userCreatorService.createUser()
        anonymousToken = userCreatorService.login(anonymous.username, anonymous.password)
    }

    @Test
    @Order(10)
    fun testGetSpaceAndNotFound() {
        val request = MockMvcRequestBuilders.get("/spaces/-1").header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("NotFoundError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.type").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.id").value("-1"))
    }

    fun createSpace(creatorToken: String, spaceName: String, spaceIntro: String, spaceAvatarId: IdType): IdType {
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
            """)
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
        val spaceId =
                JSONObject(response.andReturn().response.contentAsString)
                        .getJSONObject("data")
                        .getJSONObject("space")
                        .getLong("id")
        logger.info("Created space: $spaceId")
        return spaceId
    }

    @Test
    @Order(20)
    fun testCreateSpace() {
        createSpace(creatorToken, "$spaceName previous", spaceIntro, spaceAvatarId)
        spaceId = createSpace(creatorToken, spaceName, spaceIntro, spaceAvatarId)
        spaceIdOfSecond = createSpace(creatorToken, "$spaceName 01", spaceIntro, spaceAvatarId)
        createSpace(creatorToken, "$spaceName 02", spaceIntro, spaceAvatarId)
        spaceIdOfBeforeLast = createSpace(creatorToken, "$spaceName 03", spaceIntro, spaceAvatarId)
        spaceIdOfLast = createSpace(creatorToken, "$spaceName 04", spaceIntro, spaceAvatarId)
    }

    @Test
    @Order(30)
    fun testGetSpace() {
        val request = MockMvcRequestBuilders.get("/spaces/$spaceId").header("Authorization", "Bearer $creatorToken")
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
    }

    @Test
    @Order(40)
    fun testNameAlreadyExists() {
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
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isConflict)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("NameAlreadyExistsError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.type").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.name").value(spaceName))
    }

    @Test
    @Order(50)
    fun testPatchSpaceWithEmptyRequest() {
        val request =
                MockMvcRequestBuilders.patch("/spaces/$spaceId")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content("{}")
        val response =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
    }

    @Test
    @Order(60)
    fun testPatchSpaceWithFullRequest() {
        spaceName += " (Updated)"
        spaceIntro += " (Updated)"
        spaceAvatarId += 1
        val request =
                MockMvcRequestBuilders.patch("/spaces/$spaceId")
                        .header("Authorization", "Bearer $creatorToken")
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
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
    }

    @Test
    @Order(70)
    fun testPatchSpaceWithAnonymous() {
        val request =
                MockMvcRequestBuilders.patch("/spaces/$spaceId")
                        .header("Authorization", "Bearer $anonymousToken")
                        .contentType("application/json")
                        .content("{}")
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("modify"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceId").value(spaceId))
    }

    @Test
    @Order(75)
    fun testEnumerateSpacesByDefault() {
        val requestBuilders =
                MockMvcRequestBuilders.get("/spaces")
                        .param("page_size", "5")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(requestBuilders)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].name").value("$originalSpaceName 04"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[1].name").value("$originalSpaceName 03"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[2].name").value("$originalSpaceName 02"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[3].name").value("$originalSpaceName 01"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[4].name").value(spaceName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(spaceIdOfLast))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").isEmpty)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.next_start").isNotEmpty)
    }

    @Test
    @Order(76)
    fun testEnumerateSpacesByUpdatedAtDescWithoutStart() {
        val requestBuilders =
                MockMvcRequestBuilders.get("/spaces")
                        .param("sort_by", "updatedAt")
                        .param("sort_order", "desc")
                        .param("page_size", "4")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(requestBuilders)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].name").value(spaceName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[1].name").value("$originalSpaceName 04"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[2].name").value("$originalSpaceName 03"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[3].name").value("$originalSpaceName 02"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(spaceId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").isEmpty)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.next_start").value(spaceIdOfSecond))
    }

    @Test
    @Order(77)
    fun testEnumerateSpacesByUpdatedAtDescWithStart() {
        val requestBuilders =
                MockMvcRequestBuilders.get("/spaces")
                        .param("page_start", spaceIdOfLast.toString())
                        .param("sort_by", "updatedAt")
                        .param("sort_order", "desc")
                        .param("page_size", "1")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(requestBuilders)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.spaces[0].name").value("$originalSpaceName 04"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(spaceIdOfLast))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").value(spaceId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.next_start").value(spaceIdOfBeforeLast))
    }

    @Test
    @Order(80)
    fun testAddSpaceAdmin() {
        val request =
                MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "role": "ADMIN",
                    "userId": ${admin.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].role").value("OWNER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[1].role").value("ADMIN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[0].user.id").value(creator.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.admins[1].user.id").value(admin.userId))
    }

    @Test
    @Order(90)
    fun testAddSpaceAdminAndShipOwnership() {
        val request =
                MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "role": "OWNER",
                    "userId": ${newOwner.userId}
                }
            """)
        val result =
                mockMvc.perform(request)
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
    }

    @Test
    @Order(100)
    fun testAddSpaceAdminAfterNotOwner() {
        val request =
                MockMvcRequestBuilders.post("/spaces/$spaceId/managers")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "role": "ADMIN",
                    "userId": ${admin.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("add-admin"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("space"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceId").value(spaceId))
    }

    @Test
    @Order(110)
    fun testShipSpaceOwnership() {
        val request =
                MockMvcRequestBuilders.patch("/spaces/$spaceId/managers/${creator.userId}")
                        .header("Authorization", "Bearer $newOwnerToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                    "role": "OWNER"
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.id").value(spaceId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.name").value(spaceName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.intro").value(spaceIntro))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.space.avatarId").value(spaceAvatarId))
    }

    @Test
    @Order(120)
    fun testRemoveAdmin() {
        val request =
                MockMvcRequestBuilders.delete("/spaces/$spaceId/managers/${newOwner.userId}")
                        .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(130)
    fun testDeleteSpace() {
        val request = MockMvcRequestBuilders.delete("/spaces/$spaceId").header("Authorization", "Bearer $adminToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }
}
