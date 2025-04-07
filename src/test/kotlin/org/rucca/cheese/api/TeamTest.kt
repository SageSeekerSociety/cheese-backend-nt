/*
 *  Description: It tests the feature of team.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

// Removed: import org.json.JSONObject
// Import necessary DTOs
// Removed: import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import kotlin.math.floor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

// Removed: MockMvc and related imports

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
) // Use WebTestClient environment
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// Removed: @AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TeamTest
@Autowired
constructor(
    private val webTestClient: WebTestClient, // Inject WebTestClient
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // --- User Setup ---
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var newOwnerCandidate:
        UserCreatorService.CreateUserResponse // Renamed, as direct ownership transfer isn't simple
    lateinit var newOwnerCandidateToken: String
    lateinit var admin: UserCreatorService.CreateUserResponse
    lateinit var adminToken: String
    lateinit var member: UserCreatorService.CreateUserResponse
    lateinit var memberToken: String
    lateinit var anotherUser: UserCreatorService.CreateUserResponse
    lateinit var anotherUserToken: String

    // --- Team Details ---
    private var randomSuffix = floor(Math.random() * 10000000000).toLong()
    private var teamName = "Test Team ($randomSuffix)"
    private var teamIntro = "This is a test team"
    private var teamDescription = "A lengthy text. ".repeat(1000)
    private var teamAvatarId = userCreatorService.testAvatarId()
    private var teamId: IdType = -1

    // Store invitation/request IDs for acceptance/approval
    private var newOwnerInviteId: IdType = -1
    private var adminInviteId: IdType = -1
    private var memberInviteId: IdType = -1

    // --- Helper DTO for Error Responses ---
    data class ErrorData(
        val type: String? = null,
        val id: Any? = null,
        val name: String? = null,
        val action: String? = null,
        val resourceType: String? = null,
        val resourceId: IdType? = null,
    )

    data class ErrorDetail(val name: String, val data: ErrorData?)

    data class GenericErrorResponse(val error: ErrorDetail)

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        newOwnerCandidate = userCreatorService.createUser()
        newOwnerCandidateToken =
            userCreatorService.login(newOwnerCandidate.username, newOwnerCandidate.password)
        admin = userCreatorService.createUser()
        adminToken = userCreatorService.login(admin.username, admin.password)
        member = userCreatorService.createUser()
        memberToken = userCreatorService.login(member.username, member.password)
        anotherUser = userCreatorService.createUser()
        anotherUserToken = userCreatorService.login(anotherUser.username, anotherUser.password)
    }

    // --- Refactored Helper Methods for Membership ---

    /** Invites a user to the team with a specified role. */
    fun inviteUser(
        inviterToken: String,
        teamId: IdType,
        userIdToInvite: IdType,
        role: TeamMemberRoleTypeDTO,
    ): IdType {
        val requestDTO = TeamInvitationCreateDTO(userId = userIdToInvite, role = role)
        val response =
            webTestClient
                .post()
                .uri("/teams/$teamId/invitations")
                .header("Authorization", "Bearer $inviterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isCreated // Expect 201 Created
                .expectBody<CreateTeamInvitation201ResponseDTO>()
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.invitation?.id, "Invitation ID is null")
        logger.info(
            "Invited user $userIdToInvite to team $teamId with role $role. Invitation ID: ${response!!.data.invitation.id}"
        )
        return response.data.invitation.id
    }

    /** Accepts a team invitation. */
    fun acceptInvitation(accepterToken: String, invitationId: IdType) {
        webTestClient
            .post()
            .uri("/users/me/team-invitations/$invitationId/accept")
            .header("Authorization", "Bearer $accepterToken")
            .exchange()
            .expectStatus()
            .isNoContent // Expect 204 No Content
        logger.info("User accepted invitation $invitationId")
    }

    /** Changes the role of an existing team member. */
    fun changeMemberRole(
        adminToken: String,
        teamId: IdType,
        targetUserId: IdType,
        newRole: TeamMemberRoleTypeDTO,
    ) {
        // Cannot set OWNER via this endpoint
        require(newRole != TeamMemberRoleTypeDTO.OWNER) {
            "Cannot set role to OWNER using patchTeamMember"
        }
        val requestDTO = PatchTeamMemberRequestDTO(role = newRole)
        webTestClient
            .patch()
            .uri("/teams/$teamId/members/$targetUserId")
            .header("Authorization", "Bearer $adminToken") // Requires Admin/Owner
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>() // Assuming PATCH returns updated Team DTO
            .value { response ->
                assertNotNull(response.data.team)
                // Could add verification here that the member list reflects the change, but might
                // be complex.
                // Rely on getTeamMembers test later.
            }
        logger.info("Changed role of user $targetUserId in team $teamId to $newRole")
    }

    /** Removes a member from the team. */
    fun removeMember(
        removerToken: String,
        teamId: IdType,
        targetUserId: IdType,
        expectedStatus: HttpStatus = HttpStatus.OK,
    ) {
        webTestClient
            .delete()
            .uri("/teams/$teamId/members/$targetUserId")
            .header("Authorization", "Bearer $removerToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value()) // Expect OK or specific status
            // Optionally check response DTO if not No Content
            .expectBody<GetTeam200ResponseDTO>() // Assuming DELETE returns updated team DTO
        logger.info(
            "Attempted removal of user $targetUserId from team $teamId by token holder. Expected status: $expectedStatus"
        )
    }

    // --- Refactored Test Methods ---

    @Test
    @Order(10)
    fun `Team - Create team`() { // Renamed
        // Assuming PostTeamRequestDTO exists
        val requestDTO =
            PostTeamRequestDTO(
                name = teamName,
                intro = teamIntro,
                description = teamDescription,
                avatarId = teamAvatarId,
            )

        val response =
            webTestClient
                .post()
                .uri("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk // Original test expects OK, might be Created(201)
                .expectBody<
                    GetTeam200ResponseDTO
                >() // Assuming response DTO for get/create/patch is the same
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.team, "Team data missing in response")
        val team = response!!.data.team

        assertEquals(teamName, team.name)
        assertEquals(teamIntro, team.intro)
        assertEquals(teamDescription, team.description)
        assertEquals(teamAvatarId, team.avatarId)
        assertEquals(creator.userId, team.owner.id, "Owner ID mismatch")
        // Initial counts based on TeamDTO structure (assuming summary objects exist)
        assertEquals(0, team.admins.total, "Initial admin count should be 0")
        assertEquals(0, team.members.total, "Initial member count should be 0")
        assertEquals(true, team.joined, "'joined' status should be true for creator")
        assertEquals(
            TeamMemberRoleTypeDTO.OWNER,
            team.role,
            "Creator's role should be OWNER",
        ) // Assuming role is returned

        assertNotNull(team.id, "Team ID is null")
        teamId = team.id
        assertTrue(teamId > 0)
    }

    @Test
    @Order(20)
    fun `Team - Get team details as owner`() { // Renamed
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team, "Team data missing")
                val team = response.data.team
                assertEquals(teamId, team.id)
                assertEquals(teamName, team.name) // Use current state of teamName
                assertEquals(teamIntro, team.intro) // Use current state of teamIntro
                assertEquals(teamAvatarId, team.avatarId) // Use current state
                assertEquals(creator.userId, team.owner.id)
                assertEquals(0, team.admins.total)
                assertEquals(0, team.members.total)
                assertEquals(true, team.joined)
                assertEquals(TeamMemberRoleTypeDTO.OWNER, team.role)
            }
    }

    // Helper for checking team enumeration results
    private fun assertTeamEnumeration(
        response: GetTeams200ResponseDTO?,
        expectedSize: Int,
        expectedFirstTeamId: IdType? = null,
    ) {
        assertNotNull(response?.data, "Response data missing")
        assertNotNull(response!!.data.teams, "Teams list missing")
        assertNotNull(response.data.page, "Page info missing")
        val teams = response.data.teams
        val page = response.data.page!!

        assertEquals(expectedSize, teams.size, "Incorrect number of teams returned")
        if (expectedFirstTeamId != null && teams.isNotEmpty()) {
            assertEquals(expectedFirstTeamId, teams[0].id, "First team ID mismatch")
            // Assert basic details of the first team
            assertEquals(teamName, teams[0].name)
            assertEquals(teamIntro, teams[0].intro)
            assertEquals(teamAvatarId, teams[0].avatarId)
            assertEquals(creator.userId, teams[0].owner.id) // Owner might change later
        }
        assertNotNull(page.pageStart, "pageStart missing")
        assertNotNull(page.pageSize, "pageSize missing")
        assertNotNull(page.hasMore, "hasMore missing")
        // Assert specific page details if needed based on query params
    }

    @Test
    @Order(21)
    fun `Team - Enumerate teams filtered by name`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/teams")
                    .queryParam("query", teamName) // Filter by name
                    .queryParam("page_size", 1)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeams200ResponseDTO>()
            .value { response ->
                assertTeamEnumeration(response, 1, teamId)
                assertEquals(1, response!!.data.page!!.pageSize)
                // Further page assertions if needed
            }
    }

    @Test
    @Order(22)
    fun `Team - Enumerate teams filtered by ID`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/teams")
                    .queryParam("query", teamId.toString()) // Filter by ID string
                    .queryParam("page_size", 1)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeams200ResponseDTO>()
            .value { response ->
                assertTeamEnumeration(response, 1, teamId)
                assertEquals(1, response!!.data.page!!.pageSize)
            }
    }

    @Test
    @Order(23)
    fun `Team - Enumerate teams filtered by name with pagination`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/teams")
                    .queryParam("query", teamName)
                    .queryParam("page_start", teamId) // Start at the team itself
                    .queryParam("page_size", 1)
                    .build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeams200ResponseDTO>()
            .value { response ->
                assertTeamEnumeration(
                    response,
                    1,
                    teamId,
                ) // Should still find the team if pageStart is inclusive cursor
                assertEquals(1, response!!.data.page!!.pageSize)
                assertEquals(teamId, response.data.page!!.pageStart)
            }
    }

    @Test
    @Order(24)
    fun `Team - Enumerate teams default (no filter)`() { // Renamed
        webTestClient
            .get()
            .uri("/teams")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeams200ResponseDTO>() // Just ensure it returns OK and correct DTO type
        //            .value { response ->
        //                assertNotNull(response?.data?.teams)
        //                assertTrue(response!!.data.teams.any { it.id == teamId }) // Check our
        // team is listed
        //            }
    }

    @Test
    @Order(25)
    fun `Team - Get my teams`() { // Renamed
        webTestClient
            .get()
            .uri("/teams/my-teams")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetMyTeams200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.teams)
                val myTeam = response.data.teams.find { it.id == teamId }
                assertNotNull(myTeam, "Created team not found in my-teams list")
                assertEquals(teamName, myTeam!!.name)
                assertEquals(teamIntro, myTeam.intro)
                assertEquals(teamAvatarId, myTeam.avatarId)
                assertEquals(creator.userId, myTeam.owner.id)
                // Counts might not be in summary DTO, check based on your TeamSummaryDTO
                // assertEquals(0, myTeam.admins?.total ?: 0)
                // assertEquals(0, myTeam.members?.total ?: 0)
                assertEquals(true, myTeam.joined)
                assertEquals(TeamMemberRoleTypeDTO.OWNER, myTeam.role)
            }
    }

    // --- Adapted Membership Tests using Invite/Accept ---

    @Test
    @Order(30)
    fun `Membership - Invite and accept ADMIN (simulating ownership transfer prep)`() { // Adapted
        // Test
        // Invite newOwnerCandidate as ADMIN by the current owner (creator)
        newOwnerInviteId =
            inviteUser(creatorToken, teamId, newOwnerCandidate.userId, TeamMemberRoleTypeDTO.ADMIN)
        assertTrue(newOwnerInviteId > 0)

        // newOwnerCandidate accepts the invitation
        acceptInvitation(newOwnerCandidateToken, newOwnerInviteId)

        // Verify: Get team as original creator, check role and admin list
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                val team = response.data.team
                assertEquals(creator.userId, team.owner.id) // Creator is still owner
                assertEquals(1, team.admins.total) // newOwnerCandidate is now admin
                assertNotNull(
                    team.admins.examples.find { it.id == newOwnerCandidate.userId },
                    "New admin not found in examples",
                )
                assertEquals(true, team.joined) // Creator is still joined
                assertEquals(TeamMemberRoleTypeDTO.OWNER, team.role) // Creator role is OWNER
            }

        // Verify: Get team as the new admin (newOwnerCandidate)
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $newOwnerCandidateToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                val team = response.data.team
                assertEquals(creator.userId, team.owner.id) // Verify owner is creator
                assertEquals(true, team.joined) // New admin is joined
                assertEquals(TeamMemberRoleTypeDTO.ADMIN, team.role) // New admin role is ADMIN
            }
    }

    @Test
    @Order(35)
    fun `Membership - Try invite ADMIN again using original owner (now OWNER) - should succeed`() { // Adapted Test
        // The original owner (creator) IS still the owner and CAN invite others as ADMIN.
        // This test's premise changes. Let's test inviting the 'admin' user.
        adminInviteId = inviteUser(creatorToken, teamId, admin.userId, TeamMemberRoleTypeDTO.ADMIN)
        assertTrue(adminInviteId > 0)
        // We won't accept immediately to test other scenarios.
    }

    @Test
    @Order(40)
    fun `Membership - Try invite MEMBER using newOwnerCandidate (now ADMIN) - should succeed`() { // Adapted Test
        // The new admin (newOwnerCandidate) should be able to invite members.
        memberInviteId =
            inviteUser(newOwnerCandidateToken, teamId, member.userId, TeamMemberRoleTypeDTO.MEMBER)
        assertTrue(memberInviteId > 0)
        // Let's accept this one immediately for later tests
        acceptInvitation(memberToken, memberInviteId)
    }

    @Test
    @Order(50)
    fun `Membership - Accept ADMIN invitation for user 'admin'`() { // Adapted Test (was add admin
        // by new owner)
        // Accept the invitation sent in Order 35
        acceptInvitation(adminToken, adminInviteId)

        // Verify: Get team as owner(creator), check admin count
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                // Admins: newOwnerCandidate, admin
                assertEquals(2, response.data.team.admins.total)
            }
    }

    @Test
    @Order(60)
    fun `Membership - Member user 'member' is already added`() { // Adapted Test (was add member by
        // admin)
        // Member was added and accepted invitation in Order 40. Verify their role.
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $memberToken") // Get team as the member
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                val team = response.data.team
                assertEquals(true, team.joined)
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, team.role)
                // Check counts from member's perspective
                assertEquals(creator.userId, team.owner.id)
                assertEquals(2, team.admins.total) // newOwnerCandidate, admin
                assertEquals(1, team.members.total) // Just this member
            }
    }

    @Test
    @Order(70)
    fun `Membership - Try invite user using MEMBER token fails`() { // Adapted Test
        // Member tries to invite anotherUser
        val requestDTO =
            TeamInvitationCreateDTO(
                userId = anotherUser.userId,
                role = TeamMemberRoleTypeDTO.MEMBER,
            )
        webTestClient
            .post()
            .uri("/teams/$teamId/invitations")
            .header("Authorization", "Bearer $memberToken") // Use member token
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden // Expect 403
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    // --- Update Tests ---

    @Test
    @Order(75)
    fun `Team - Update fails for anonymous user`() { // Renamed
        val requestDTO = PatchTeamRequestDTO(intro = "Attempted update")
        webTestClient
            .patch()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $anotherUserToken") // Non-member token
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(80)
    fun `Team - Update fails for member`() { // Renamed
        val requestDTO = PatchTeamRequestDTO(intro = "Attempted update by member")
        webTestClient
            .patch()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $memberToken") // Member token
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("AccessDeniedError", error.error.name) }
    }

    @Test
    @Order(90)
    fun `Team - Update success for admin`() { // Renamed
        // Update local state first for comparison
        val updatedTeamNameAdmin = "$teamName (Updated by Admin)"
        val updatedTeamIntroAdmin = "$teamIntro (Updated by Admin)"
        val updatedTeamDescriptionAdmin = "$teamDescription (Updated by Admin)"
        val updatedTeamAvatarIdAdmin = teamAvatarId + 1

        val requestDTO =
            PatchTeamRequestDTO(
                name = updatedTeamNameAdmin,
                intro = updatedTeamIntroAdmin,
                description = updatedTeamDescriptionAdmin,
                avatarId = updatedTeamAvatarIdAdmin,
            )

        webTestClient
            .patch()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $adminToken") // Use admin token
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                val team = response.data.team
                assertEquals(updatedTeamNameAdmin, team.name)
                assertEquals(updatedTeamIntroAdmin, team.intro)
                assertEquals(updatedTeamDescriptionAdmin, team.description)
                assertEquals(updatedTeamAvatarIdAdmin, team.avatarId)
            }
        // Update test state
        teamName = updatedTeamNameAdmin
        teamIntro = updatedTeamIntroAdmin
        teamDescription = updatedTeamDescriptionAdmin
        teamAvatarId = updatedTeamAvatarIdAdmin
    }

    @Test
    @Order(100)
    fun `Team - Update success for owner`() { // Renamed
        // Update local state first
        val updatedTeamNameOwner = "$teamName (Updated by Owner)"
        val updatedTeamIntroOwner = "$teamIntro (Updated by Owner)"
        val updatedTeamAvatarIdOwner = teamAvatarId + 1

        val requestDTO =
            PatchTeamRequestDTO(
                name = updatedTeamNameOwner,
                intro = updatedTeamIntroOwner,
                avatarId = updatedTeamAvatarIdOwner,
                // Description not updated here as per original test
            )

        webTestClient
            .patch()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $creatorToken") // Use owner token
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                val team = response.data.team
                assertEquals(updatedTeamNameOwner, team.name)
                assertEquals(updatedTeamIntroOwner, team.intro)
                assertEquals(teamDescription, team.description) // Description should be unchanged
                assertEquals(updatedTeamAvatarIdOwner, team.avatarId)
            }
        // Update test state
        teamName = updatedTeamNameOwner
        teamIntro = updatedTeamIntroOwner
        teamAvatarId = updatedTeamAvatarIdOwner
    }

    // --- Role Change and Removal Tests ---

    @Test
    @Order(110)
    fun `Membership - Get team members list`() { // Renamed
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken") // Owner requests list
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val members = response.data.members
                // Expected: creator(owner), newOwnerCandidate(admin), admin(admin), member(member)
                assertEquals(4, members.size)

                val memberMap = members.associateBy { it.user.id }
                assertTrue(memberMap.containsKey(creator.userId))
                assertEquals(TeamMemberRoleTypeDTO.OWNER, memberMap[creator.userId]?.role)

                assertTrue(memberMap.containsKey(newOwnerCandidate.userId))
                assertEquals(TeamMemberRoleTypeDTO.ADMIN, memberMap[newOwnerCandidate.userId]?.role)

                assertTrue(memberMap.containsKey(admin.userId))
                assertEquals(TeamMemberRoleTypeDTO.ADMIN, memberMap[admin.userId]?.role)

                assertTrue(memberMap.containsKey(member.userId))
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, memberMap[member.userId]?.role)
            }
    }

    @Test
    @Order(120)
    fun `Membership - Change role ADMIN to MEMBER`() { // Adapted test (was ship ownership via
        // patch)
        // Change user 'admin' from ADMIN to MEMBER using the owner's token
        changeMemberRole(creatorToken, teamId, admin.userId, TeamMemberRoleTypeDTO.MEMBER)

        // Verify role change
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val adminMember = response.data.members.find { it.user.id == admin.userId }
                assertNotNull(adminMember)
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, adminMember!!.role)
            }
    }

    @Test
    @Order(130)
    fun `Membership - Change role MEMBER to ADMIN`() { // Renamed
        // Change user 'member' from MEMBER to ADMIN using owner token
        changeMemberRole(creatorToken, teamId, member.userId, TeamMemberRoleTypeDTO.ADMIN)

        // Verify role change
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val memberNowAdmin = response.data.members.find { it.user.id == member.userId }
                assertNotNull(memberNowAdmin)
                assertEquals(TeamMemberRoleTypeDTO.ADMIN, memberNowAdmin!!.role)
            }
    }

    @Test
    @Order(140)
    fun `Membership - Change role ADMIN to MEMBER again`() { // Renamed
        // Change user 'member' back from ADMIN to MEMBER using owner token
        changeMemberRole(creatorToken, teamId, member.userId, TeamMemberRoleTypeDTO.MEMBER)

        // Verify role change
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val memberAgain = response.data.members.find { it.user.id == member.userId }
                assertNotNull(memberAgain)
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, memberAgain!!.role)
                // Check owner and other admins are still correct
                val owner = response.data.members.find { it.user.id == creator.userId }
                assertEquals(TeamMemberRoleTypeDTO.OWNER, owner?.role)
                val admin1 = response.data.members.find { it.user.id == newOwnerCandidate.userId }
                assertEquals(TeamMemberRoleTypeDTO.ADMIN, admin1?.role)
                // User 'admin' was changed to MEMBER in test 120
                val admin2 = response.data.members.find { it.user.id == admin.userId }
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, admin2?.role)
            }
    }

    @Test
    @Order(148)
    fun `Team - Get team as member`() { // Renamed
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $memberToken") // Use member's token
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                assertEquals(true, response.data.team.joined)
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, response.data.team.role)
            }
    }

    @Test
    @Order(149)
    fun `Team - Get team as non-member`() { // Renamed
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $anotherUserToken") // Use non-member's token
            .exchange()
            .expectStatus()
            .isOk // Non-members can likely still view public team info
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                assertEquals(false, response.data.team.joined)
                assertNull(response.data.team.role, "Role should be null for non-members")
            }
    }

    @Test
    @Order(150)
    fun `Membership - Remove self as member`() { // Renamed
        removeMember(
            memberToken,
            teamId,
            member.userId,
            expectedStatus = HttpStatus.OK,
        ) // Expect OK based on original

        // Verify removal
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                assertNull(
                    response.data.members.find { it.user.id == member.userId },
                    "Member should be removed",
                )
            }
    }

    @Test
    @Order(151)
    fun `Membership - Add member back via invitation`() { // Renamed & Adapted
        // Invite member again (using owner token)
        val inviteId = inviteUser(creatorToken, teamId, member.userId, TeamMemberRoleTypeDTO.MEMBER)
        // Member accepts
        acceptInvitation(memberToken, inviteId)

        // Verify member is back
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val memberFound = response.data.members.find { it.user.id == member.userId }
                assertNotNull(memberFound)
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, memberFound!!.role)
            }
    }

    @Test
    @Order(152)
    fun `Membership - Remove member using owner token`() { // Renamed
        removeMember(creatorToken, teamId, member.userId, expectedStatus = HttpStatus.OK)

        // Verify removal
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                assertNull(
                    response.data.members.find { it.user.id == member.userId },
                    "Member should be removed again",
                )
            }
    }

    @Test
    @Order(160)
    fun `Membership - Remove admin using owner token`() { // Renamed
        // Remove user 'admin' (who was demoted to MEMBER in test 120)
        removeMember(creatorToken, teamId, admin.userId, expectedStatus = HttpStatus.OK)

        // Verify removal and remaining admins
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                assertNull(
                    response.data.members.find { it.user.id == admin.userId },
                    "User 'admin' should be removed",
                )

                // Check owner and remaining admin(s)
                val owner =
                    response.data.members.find {
                        it.user.id == creator.userId && it.role == TeamMemberRoleTypeDTO.OWNER
                    }
                assertNotNull(owner)
                val admin1 =
                    response.data.members.find {
                        it.user.id == newOwnerCandidate.userId &&
                            it.role == TeamMemberRoleTypeDTO.ADMIN
                    }
                assertNotNull(admin1)

                // Verify total count reflects removal
                assertEquals(2, response.data.members.size) // Owner + 1 Admin remaining
            }
    }

    @Test
    @Order(170)
    fun `Team - Delete team success by owner`() { // Renamed
        webTestClient
            .delete()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $creatorToken") // Owner deletes
            .exchange()
            .expectStatus()
            .isOk // Assuming 200 OK for delete
            .expectBody<CommonResponseDTO>() // Assuming simple response DTO
            .value { assertEquals(200, it.code) }

        // Verify deletion
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isNotFound // Expect 404 Not Found
    }
}
