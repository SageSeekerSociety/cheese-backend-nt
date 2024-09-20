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
    lateinit var newOwner: UserCreatorService.CreateUserResponse
    lateinit var newOwnerToken: String
    lateinit var admin: UserCreatorService.CreateUserResponse
    lateinit var adminToken: String
    lateinit var member: UserCreatorService.CreateUserResponse
    lateinit var memberToken: String
    lateinit var anotherUser: UserCreatorService.CreateUserResponse
    lateinit var anotherUserToken: String
    private var teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})"
    private var teamIntro = "This is a test team"
    private var teamAvatarId = userCreatorService.testAvatarId()
    private var teamId: IdType = -1

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        newOwner = userCreatorService.createUser()
        newOwnerToken = userCreatorService.login(newOwner.username, newOwner.password)
        admin = userCreatorService.createUser()
        adminToken = userCreatorService.login(admin.username, admin.password)
        member = userCreatorService.createUser()
        memberToken = userCreatorService.login(member.username, member.password)
        anotherUser = userCreatorService.createUser()
        anotherUserToken = userCreatorService.login(anotherUser.username, anotherUser.password)
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

    @Test
    @Order(30)
    fun testShipTeamOwnership() {
        val request =
                MockMvcRequestBuilders.post("/teams/$teamId/members")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "role": "OWNER",
                  "user_id": ${newOwner.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[0].id").value(newOwner.userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[1].id").value(creator.userId))
    }

    @Test
    @Order(35)
    fun testShipTeamOwnershipUseOldOwnerAndGetPermissionDeniedError() {
        val request =
                MockMvcRequestBuilders.post("/teams/$teamId/members")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "role": "OWNER",
                  "user_id": ${admin.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("ship-ownership"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("team"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceId").value(teamId))
    }

    @Test
    @Order(40)
    fun testAddAdminUseOriginalOwnerAndGetPermissionDeniedError() {
        val request =
                MockMvcRequestBuilders.post("/teams/$teamId/members")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "role": "ADMIN",
                  "user_id": ${admin.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("add-admin"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("team"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceId").value(teamId))
    }

    @Test
    @Order(50)
    fun testAddAdminUseNewOwner() {
        val request =
                MockMvcRequestBuilders.post("/teams/$teamId/members")
                        .header("Authorization", "Bearer $newOwnerToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "role": "ADMIN",
                  "user_id": ${admin.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[0].id").value(newOwner.userId))
    }

    @Test
    @Order(60)
    fun testAddMemberUseAdmin() {
        val request =
                MockMvcRequestBuilders.post("/teams/$teamId/members")
                        .header("Authorization", "Bearer $adminToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "role": "MEMBER",
                  "user_id": ${member.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.examples[0].id").value(member.userId))
    }

    @Test
    @Order(70)
    fun testAddMemberUseMemberAndGetPermissionDeniedError() {
        val request =
                MockMvcRequestBuilders.post("/teams/$teamId/members")
                        .header("Authorization", "Bearer $memberToken")
                        .contentType("application/json")
                        .content(
                                """
                {
                  "role": "MEMBER",
                  "user_id": ${anotherUser.userId}
                }
            """)
        mockMvc.perform(request)
                .andExpect(MockMvcResultMatchers.status().isForbidden)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("add-normal-member"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("team"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceId").value(teamId))
    }
}
