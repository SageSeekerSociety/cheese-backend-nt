/*
 *  Description: It tests the feature of team.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.UserCreatorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TeamTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {
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
    private var teamDescription = "A lengthy text. ".repeat(1000)
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
                  "description": "$teamDescription",
                  "avatarId": $teamAvatarId
                }
            """
                )
        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.name").value(teamName))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.intro").value(teamIntro))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.team.description").value(teamDescription)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.team.avatarId").value(teamAvatarId)
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.data.team.owner.id").value(creator.userId)
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("OWNER"))
        teamId =
            JSONObject(response.andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("team")
                .getLong("id")
    }

    @Test
    @Order(20)
    fun testGetTeam() {
        val request =
            MockMvcRequestBuilders.get("/teams/$teamId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.intro").value(teamIntro))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.avatarId").value(teamAvatarId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.owner.id").value(creator.userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("OWNER"))
    }

    @Test
    @Order(21)
    fun testEnumerateTeams() {
        val request =
            MockMvcRequestBuilders.get("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .queryParam("query", teamName)
                .queryParam("page_size", "1")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].id").value(teamId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].intro").value(teamIntro))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].avatarId").value(teamAvatarId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].owner.id").value(creator.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].admins.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].members.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(teamId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").doesNotExist())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").isBoolean())
    }

    @Test
    @Order(22)
    fun testEnumerateTeamsWithId() {
        val request =
            MockMvcRequestBuilders.get("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .queryParam("query", teamId.toString())
                .queryParam("page_size", "1")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].id").value(teamId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].intro").value(teamIntro))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].avatarId").value(teamAvatarId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].owner.id").value(creator.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].admins.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].members.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(teamId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").doesNotExist())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").isBoolean())
    }

    @Test
    @Order(23)
    fun testEnumerateTeams2() {
        val request =
            MockMvcRequestBuilders.get("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .queryParam("query", teamName)
                .queryParam("page_start", teamId.toString())
                .queryParam("page_size", "1")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].id").value(teamId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].intro").value(teamIntro))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].avatarId").value(teamAvatarId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].owner.id").value(creator.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].admins.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].members.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_start").value(teamId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.page_size").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_prev").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.prev_start").doesNotExist())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page.has_more").isBoolean())
    }

    @Test
    @Order(24)
    fun testEnumerateTeamsByDefault() {
        val request =
            MockMvcRequestBuilders.get("/teams").header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(25)
    fun testGetMyTeams() {
        val request =
            MockMvcRequestBuilders.get("/teams/my-teams")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].intro").value(teamIntro))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].avatarId").value(teamAvatarId)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.teams[0].owner.id").value(creator.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].admins.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].members.total").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.teams[0].role").value("OWNER"))
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
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.owner.id").value(newOwner.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[0].id")
                    .value(creator.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("ADMIN"))
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
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.data.action").value("ship-ownership")
            )
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
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
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
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.owner.id").value(newOwner.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("OWNER"))
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
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.members.examples[0].id")
                    .value(member.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("ADMIN"))
    }

    @Test
    @Order(70)
    fun testAddMemberUseAnotherUserAndGetPermissionDeniedError() {
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
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.data.action").value("add-normal-member")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("team"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceId").value(teamId))
    }

    @Test
    @Order(75)
    fun testUpdateTeamUseAnonymousAndGetPermissionDeniedError() {
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId")
                .header("Authorization", "Bearer $anotherUserToken")
                .contentType("application/json")
                .content("{}")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("modify"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("team"))
    }

    @Test
    @Order(80)
    fun testUpdateTeamUseMemberAndGetPermissionDeniedError() {
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId")
                .header("Authorization", "Bearer $memberToken")
                .contentType("application/json")
                .content("{}")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.error.name").value("PermissionDeniedError")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.action").value("modify"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.error.data.resourceType").value("team"))
    }

    @Test
    @Order(90)
    fun testUpdateTeamUseAdmin() {
        teamName = "$teamName (2)"
        teamIntro = "$teamIntro (2)"
        teamDescription = "$teamDescription (2)"
        teamAvatarId += 1
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId")
                .header("Authorization", "Bearer $adminToken")
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
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.intro").value(teamIntro))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.description").value(teamDescription)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.avatarId").value(teamAvatarId))
    }

    @Test
    @Order(100)
    fun testUpdateTeamUseOwner() {
        teamName = "$teamName (2)"
        teamIntro = "$teamIntro (2)"
        teamAvatarId += 1
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId")
                .header("Authorization", "Bearer $newOwnerToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$teamName",
                  "intro": "$teamIntro",
                  "avatarId": $teamAvatarId
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.name").value(teamName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.intro").value(teamIntro))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.avatarId").value(teamAvatarId))
    }

    @Test
    @Order(110)
    fun testGetTeamMembers() {
        val request =
            MockMvcRequestBuilders.get("/teams/$teamId/members")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.members[?(@.user.id == ${newOwner.userId} && @.role == 'OWNER')]"
                    )
                    .exists()
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.members[?(@.user.id == ${admin.userId} && @.role == 'ADMIN')]"
                    )
                    .exists()
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.members[?(@.user.id == ${creator.userId} && @.role == 'ADMIN')]"
                    )
                    .exists()
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.members[?(@.user.id == ${member.userId} && @.role == 'MEMBER')]"
                    )
                    .exists()
            )
    }

    @Test
    @Order(120)
    fun testShipOwnership() {
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId/members/${creator.userId}")
                .header("Authorization", "Bearer $newOwnerToken")
                .contentType("application/json")
                .content("""{ "role": "OWNER" }""")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.team.admins.examples[?(@.id == ${newOwner.userId})]"
                    )
                    .exists()
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.team.admins.examples[?(@.id == ${admin.userId})]"
                    )
                    .exists()
            )
    }

    @Test
    @Order(130)
    fun testChangeNormalMemberToAdmin() {
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId/members/${member.userId}")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("""{ "role": "ADMIN" }""")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(3))
    }

    @Test
    @Order(140)
    fun testChangeAdminToNormalMember() {
        val request =
            MockMvcRequestBuilders.patch("/teams/$teamId/members/${member.userId}")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content("""{ "role": "MEMBER" }""")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.owner.id").value(creator.userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.team.admins.examples[?(@.id == ${admin.userId})]"
                    )
                    .exists()
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                        "$.data.team.admins.examples[?(@.id == ${newOwner.userId})]"
                    )
                    .exists()
            )
    }

    @Test
    @Order(148)
    fun testGetTeamUseMember() {
        val request =
            MockMvcRequestBuilders.get("/teams/$teamId")
                .header("Authorization", "Bearer $memberToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("MEMBER"))
    }

    @Test
    @Order(149)
    fun testGetTeamUseAnotherUser() {
        val request =
            MockMvcRequestBuilders.get("/teams/$teamId")
                .header("Authorization", "Bearer $anotherUserToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").doesNotExist())
    }

    @Test
    @Order(150)
    fun testRemoveMemberUseSelf() {
        val request =
            MockMvcRequestBuilders.delete("/teams/$teamId/members/${member.userId}")
                .header("Authorization", "Bearer $memberToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(0))
    }

    @Test
    @Order(151)
    fun testAddMemberUseSelf() {
        val request =
            MockMvcRequestBuilders.post("/teams/$teamId/members")
                .header("Authorization", "Bearer ${memberToken}")
                .contentType("application/json")
                .content(
                    """
                {
                  "role": "MEMBER",
                  "user_id": ${member.userId}
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.members.examples[0].id")
                    .value(member.userId)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.joined").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.role").value("MEMBER"))
    }

    @Test
    @Order(152)
    fun testRemoveMember() {
        val request =
            MockMvcRequestBuilders.delete("/teams/$teamId/members/${member.userId}")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.members.total").value(0))
    }

    @Test
    @Order(160)
    fun testRemoveAdmin() {
        val request =
            MockMvcRequestBuilders.delete("/teams/$teamId/members/${admin.userId}")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.owner.id").value(creator.userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.team.admins.total").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.team.admins.examples[0].id")
                    .value(newOwner.userId)
            )
    }

    @Test
    @Order(170)
    fun testDeleteTeam() {
        val request =
            MockMvcRequestBuilders.delete("/teams/$teamId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)

        val getRequest =
            MockMvcRequestBuilders.get("/teams/$teamId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(getRequest).andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
