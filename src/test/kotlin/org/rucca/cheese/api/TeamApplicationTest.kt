package org.rucca.cheese.api

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

/**
 * Tests for the Team Membership Application/Invitation flow. This class is separate from TeamTest
 * to avoid state conflicts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class TeamApplicationTest
@Autowired
constructor(
    private val webTestClient: WebTestClient,
    private val userCreatorService: UserCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // --- Users for this Test Suite ---
    lateinit var teamOwner: UserCreatorService.CreateUserResponse // Owner of the test team
    lateinit var teamOwnerToken: String
    lateinit var requesterUser: UserCreatorService.CreateUserResponse // User requesting to join
    lateinit var requesterToken: String
    lateinit var inviteeUser: UserCreatorService.CreateUserResponse // User being invited
    lateinit var inviteeToken: String
    lateinit var anotherUser:
        UserCreatorService.CreateUserResponse // Another user for conflict tests
    lateinit var anotherUserToken: String

    // --- Team for this Test Suite ---
    private var teamId: IdType = -1
    private val randomSuffix = floor(Math.random() * 10000000000).toLong()

    // --- State for Request/Invite IDs ---
    private var joinRequestId: IdType = -1
    private var joinRequestIdCanceled: IdType = -1
    private var joinRequestIdApproved: IdType = -1
    private var invitationId: IdType = -1
    private var invitationIdCanceled: IdType = -1
    private var invitationIdDeclined: IdType = -1
    private var invitationIdAccepted: IdType = -1

    // --- Helper DTO for Error Responses ---
    // (Assuming GenericErrorResponse is defined as before)
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
    fun setup() {
        // Create users
        teamOwner = userCreatorService.createUser()
        teamOwnerToken = userCreatorService.login(teamOwner.username, teamOwner.password)
        requesterUser = userCreatorService.createUser()
        requesterToken = userCreatorService.login(requesterUser.username, requesterUser.password)
        inviteeUser = userCreatorService.createUser()
        inviteeToken = userCreatorService.login(inviteeUser.username, inviteeUser.password)
        anotherUser = userCreatorService.createUser()
        anotherUserToken = userCreatorService.login(anotherUser.username, anotherUser.password)

        // Create a team specifically for these tests
        val teamName = "Application Test Team ($randomSuffix)"
        val teamIntro = "Team for testing requests/invites"
        val requestDTO =
            PostTeamRequestDTO(
                name = teamName,
                intro = teamIntro,
                description = "...",
                avatarId = userCreatorService.testAvatarId(),
            )
        val response =
            webTestClient
                .post()
                .uri("/teams")
                .header("Authorization", "Bearer $teamOwnerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk // Assuming 200 OK for creation based on TeamTest
                .expectBody<GetTeam200ResponseDTO>()
                .returnResult()
                .responseBody
        teamId = response?.data?.team?.id ?: fail("Failed to create team for application tests")
        assertTrue(teamId > 0)
        logger.info("Setup complete for TeamApplicationTest. Team ID: $teamId")
    }

    // --- Helper Methods for Application/Invitation Flow ---
    // (Copied and verified from the previous attempt, ensuring they don't rely on TeamTest state)

    /** Invites a user to the team with a specified role. */
    fun inviteUser(
        inviterToken: String,
        teamId: IdType,
        userIdToInvite: IdType,
        role: TeamMemberRoleTypeDTO,
        expectedStatus: HttpStatus = HttpStatus.CREATED, // Allow overriding for error tests
    ): IdType? {
        val requestDTO = TeamInvitationCreateDTO(userId = userIdToInvite, role = role)
        val responseSpec =
            webTestClient
                .post()
                .uri("/teams/$teamId/invitations")
                .header("Authorization", "Bearer $inviterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isEqualTo(expectedStatus.value())

        if (expectedStatus == HttpStatus.CREATED) {
            val response =
                responseSpec
                    .expectBody<CreateTeamInvitation201ResponseDTO>()
                    .returnResult()
                    .responseBody
            assertNotNull(response?.data?.invitation?.id, "Invitation ID is null")
            val id = response!!.data.invitation.id
            logger.info(
                "Invited user $userIdToInvite to team $teamId with role $role. Invitation ID: $id"
            )
            return id
        }
        return null
    }

    /** Accepts a team invitation. */
    fun acceptInvitation(
        accepterToken: String,
        invitationId: IdType,
        expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
    ) {
        webTestClient
            .post()
            .uri("/users/me/team-invitations/$invitationId/accept")
            .header("Authorization", "Bearer $accepterToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value()) // Expect 204 No Content or error
        if (expectedStatus == HttpStatus.NO_CONTENT) {
            logger.info("User accepted invitation $invitationId")
        }
    }

    /** Declines a team invitation. */
    fun declineInvitation(
        declinerToken: String,
        invitationId: IdType,
        expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
    ) {
        webTestClient
            .post()
            .uri("/users/me/team-invitations/$invitationId/decline")
            .header("Authorization", "Bearer $declinerToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value())
        if (expectedStatus == HttpStatus.NO_CONTENT) {
            logger.info("User declined invitation $invitationId")
        }
    }

    /** Requests to join a team. */
    fun requestToJoin(
        requestorToken: String,
        teamId: IdType,
        message: String? = null,
        expectedStatus: HttpStatus = HttpStatus.CREATED,
    ): IdType? {
        val requestDTO = TeamJoinRequestCreateDTO(message = message)
        val responseSpec =
            webTestClient
                .post()
                .uri("/teams/$teamId/requests")
                .header("Authorization", "Bearer $requestorToken")
                .contentType(MediaType.APPLICATION_JSON)
                // Handle potentially null body for requests without message
                .apply {
                    if (requestDTO.message != null) bodyValue(requestDTO)
                    else contentType(MediaType.APPLICATION_JSON)
                } // Ensure content type is set even for empty body
                .exchange()
                .expectStatus()
                .isEqualTo(expectedStatus.value())

        if (expectedStatus == HttpStatus.CREATED) {
            val response =
                responseSpec
                    .expectBody<CreateTeamJoinRequest201ResponseDTO>()
                    .returnResult()
                    .responseBody
            assertNotNull(response?.data?.application?.id, "Join Request ID is null")
            val id = response!!.data!!.application!!.id
            logger.info("User requested to join team $teamId. Request ID: $id")
            return id
        }
        return null
    }

    /** Cancels own join request. */
    fun cancelMyJoinRequest(
        requestorToken: String,
        requestId: IdType,
        expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
    ) {
        webTestClient
            .delete()
            .uri("/users/me/team-requests/$requestId")
            .header("Authorization", "Bearer $requestorToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value())
        if (expectedStatus == HttpStatus.NO_CONTENT) {
            logger.info("User canceled join request $requestId")
        }
    }

    /** Approves a join request. */
    fun approveJoinRequest(
        adminToken: String,
        teamId: IdType,
        requestId: IdType,
        expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
    ) {
        webTestClient
            .post()
            .uri("/teams/$teamId/requests/$requestId/approve")
            .header("Authorization", "Bearer $adminToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value())
        if (expectedStatus == HttpStatus.NO_CONTENT) {
            logger.info("Admin approved join request $requestId for team $teamId")
        }
    }

    /** Rejects a join request. */
    fun rejectJoinRequest(
        adminToken: String,
        teamId: IdType,
        requestId: IdType,
        expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
    ) {
        webTestClient
            .post()
            .uri("/teams/$teamId/requests/$requestId/reject")
            .header("Authorization", "Bearer $adminToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value())
        if (expectedStatus == HttpStatus.NO_CONTENT) {
            logger.info("Admin rejected join request $requestId for team $teamId")
        }
    }

    /** Cancels a sent invitation. */
    fun cancelInvitation(
        adminToken: String,
        teamId: IdType,
        invitationId: IdType,
        expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
    ) {
        webTestClient
            .delete()
            .uri("/teams/$teamId/invitations/$invitationId")
            .header("Authorization", "Bearer $adminToken")
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.value())
        if (expectedStatus == HttpStatus.NO_CONTENT) {
            logger.info("Admin canceled invitation $invitationId for team $teamId")
        }
    }

    // --- Request Flow Tests ---

    @Test
    @Order(10) // Start ordering from 10 for this class
    fun `Request - User requests to join team`() {
        val message = "Please let me join!"
        joinRequestId =
            requestToJoin(requesterToken, teamId, message) ?: fail("Failed to create join request")
        assertTrue(joinRequestId > 0)
    }

    @Test
    @Order(15)
    fun `Request - User lists their PENDING requests`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/users/me/team-requests")
                    .queryParam("status", ApplicationStatusDTO.PENDING.value)
                    .build()
            }
            .header("Authorization", "Bearer $requesterToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyJoinRequests200ResponseDTO>() // Adjust DTO name if needed
            .value { response ->
                assertNotNull(response.data?.requests)
                val requests = response.data!!.requests
                val foundRequest = requests.find { it.id == joinRequestId }
                assertNotNull(foundRequest, "Pending request $joinRequestId not found for user")
                assertEquals(ApplicationStatusDTO.PENDING, foundRequest!!.status)
                assertEquals(ApplicationTypeDTO.REQUEST, foundRequest.type)
                assertEquals(requesterUser.userId, foundRequest.user.id)
                assertEquals(requesterUser.userId, foundRequest.initiator.id)
                assertEquals(teamId, foundRequest.team.id)
                assertEquals("Please let me join!", foundRequest.message)
            }
    }

    @Test
    @Order(20)
    fun `Request - Admin lists PENDING requests for team`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/teams/$teamId/requests")
                    .queryParam("status", ApplicationStatusDTO.PENDING.value)
                    .build()
            }
            .header("Authorization", "Bearer $teamOwnerToken") // Use owner token
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListTeamJoinRequests200ResponseDTO>() // Adjust DTO name
            .value { response ->
                assertNotNull(response.data?.applications)
                val requests = response.data!!.applications!!
                val foundRequest = requests.find { it.id == joinRequestId }
                assertNotNull(foundRequest, "Pending request $joinRequestId not found for team")
                assertEquals(ApplicationStatusDTO.PENDING, foundRequest!!.status)
                assertEquals(ApplicationTypeDTO.REQUEST, foundRequest.type)
                assertEquals(requesterUser.userId, foundRequest.user.id) // User is the requester
                assertEquals(
                    requesterUser.userId,
                    foundRequest.initiator.id,
                ) // Initiator is the requester
            }
    }

    @Test
    @Order(25)
    fun `Request - Admin rejects the request`() {
        rejectJoinRequest(teamOwnerToken, teamId, joinRequestId)
    }

    @Test
    @Order(30)
    fun `Request - User lists their requests (check rejected)`() {
        webTestClient
            .get()
            .uri("/users/me/team-requests") // Get all statuses
            .header("Authorization", "Bearer $requesterToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyJoinRequests200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.requests)
                val request = response.data!!.requests.find { it.id == joinRequestId }
                assertNotNull(request, "Request $joinRequestId not found")
                assertEquals(
                    ApplicationStatusDTO.REJECTED,
                    request!!.status,
                    "Request status should be REJECTED",
                )
                assertEquals(
                    teamOwner.userId,
                    request.processedBy?.id,
                    "ProcessedBy should be the team owner",
                )
                assertNotNull(request.processedAt)
            }
    }

    @Test
    @Order(35)
    fun `Request - User requests to join again`() {
        // Request again after rejection
        joinRequestIdCanceled =
            requestToJoin(requesterToken, teamId, "Second request")
                ?: fail("Failed to create second join request")
        assertTrue(joinRequestIdCanceled > 0)
    }

    @Test
    @Order(40)
    fun `Request - User cancels their own pending request`() {
        cancelMyJoinRequest(requesterToken, joinRequestIdCanceled)
    }

    @Test
    @Order(45)
    fun `Request - User lists their requests (check canceled)`() {
        webTestClient
            .get()
            .uri("/users/me/team-requests")
            .header("Authorization", "Bearer $requesterToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyJoinRequests200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.requests)
                val request = response.data!!.requests.find { it.id == joinRequestIdCanceled }
                assertNotNull(request, "Request $joinRequestIdCanceled not found")
                assertEquals(
                    ApplicationStatusDTO.CANCELED,
                    request!!.status,
                    "Request status should be CANCELED",
                )
                // Canceled by self
                assertEquals(
                    requesterUser.userId,
                    request.processedBy?.id,
                    "ProcessedBy should be the requester",
                )
                assertNotNull(request.processedAt)
            }
    }

    @Test
    @Order(50)
    fun `Request - User requests to join a third time`() {
        joinRequestIdApproved =
            requestToJoin(requesterToken, teamId, "Third time lucky?")
                ?: fail("Failed to create third join request")
        assertTrue(joinRequestIdApproved > 0)
    }

    @Test
    @Order(55)
    fun `Request - Admin approves the third request`() {
        approveJoinRequest(teamOwnerToken, teamId, joinRequestIdApproved)
    }

    @Test
    @Order(60)
    fun `Request - Verify user is now a member after approval`() {
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $teamOwnerToken") // Owner checks member list
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val newMember = response.data.members.find { it.user.id == requesterUser.userId }
                assertNotNull(newMember, "User ${requesterUser.userId} should now be a member")
                // Should join with default MEMBER role from request flow
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, newMember!!.role)
            }

        // Also verify from the user's perspective
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $requesterToken") // User checks team status
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                assertEquals(true, response.data.team.joined, "User should be marked as joined")
                assertEquals(
                    TeamMemberRoleTypeDTO.MEMBER,
                    response.data.team.role,
                    "User role should be MEMBER",
                )
            }
    }

    @Test
    @Order(65)
    fun `Request - User lists their requests (check approved)`() {
        webTestClient
            .get()
            .uri("/users/me/team-requests")
            .header("Authorization", "Bearer $requesterToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyJoinRequests200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.requests)
                val request = response.data!!.requests.find { it.id == joinRequestIdApproved }
                assertNotNull(request, "Request $joinRequestIdApproved not found")
                assertEquals(
                    ApplicationStatusDTO.APPROVED,
                    request!!.status,
                    "Request status should be APPROVED",
                )
                assertEquals(
                    teamOwner.userId,
                    request.processedBy?.id,
                    "ProcessedBy should be the team owner",
                )
                assertNotNull(request.processedAt)
            }
    }

    // --- Invitation Flow Tests ---

    @Test
    @Order(100)
    fun `Invitation - Owner invites user`() {
        // `teamOwner` invites `inviteeUser` to `teamId`
        invitationIdCanceled =
            inviteUser(teamOwnerToken, teamId, inviteeUser.userId, TeamMemberRoleTypeDTO.MEMBER)
                ?: fail("Failed to create invitation")
        assertTrue(invitationIdCanceled > 0)
    }

    @Test
    @Order(105)
    fun `Invitation - Owner lists sent PENDING invitations`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/teams/$teamId/invitations")
                    .queryParam("status", ApplicationStatusDTO.PENDING.value)
                    .build()
            }
            .header("Authorization", "Bearer $teamOwnerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListTeamInvitations200ResponseDTO>() // Adjust DTO name
            .value { response ->
                assertNotNull(response.data.invitations)
                val invitations = response.data.invitations
                val foundInvite = invitations.find { it.id == invitationIdCanceled }
                assertNotNull(foundInvite, "Pending invitation $invitationIdCanceled not found")
                assertEquals(ApplicationStatusDTO.PENDING, foundInvite!!.status)
                assertEquals(ApplicationTypeDTO.INVITATION, foundInvite.type)
                assertEquals(inviteeUser.userId, foundInvite.user.id) // User is the invitee
                assertEquals(teamOwner.userId, foundInvite.initiator.id) // Initiator is the owner
                assertEquals(TeamMemberRoleTypeDTO.MEMBER, foundInvite.role)
            }
    }

    @Test
    @Order(110)
    fun `Invitation - Invitee lists received PENDING invitations`() {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/users/me/team-invitations")
                    .queryParam("status", ApplicationStatusDTO.PENDING.value)
                    .build()
            }
            .header("Authorization", "Bearer $inviteeToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyInvitations200ResponseDTO>() // Adjust DTO name
            .value { response ->
                assertNotNull(response.data?.invitations)
                val invitations = response.data!!.invitations
                val foundInvite = invitations.find { it.id == invitationIdCanceled }
                assertNotNull(
                    foundInvite,
                    "Pending invitation $invitationIdCanceled not found for invitee",
                )
                assertEquals(ApplicationStatusDTO.PENDING, foundInvite!!.status)
                assertEquals(teamId, foundInvite.team.id)
            }
    }

    @Test
    @Order(115)
    fun `Invitation - Owner cancels the pending invitation`() {
        cancelInvitation(teamOwnerToken, teamId, invitationIdCanceled)
    }

    @Test
    @Order(120)
    fun `Invitation - Invitee lists invitations (check canceled)`() {
        webTestClient
            .get()
            .uri("/users/me/team-invitations") // Get all statuses
            .header("Authorization", "Bearer $inviteeToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyInvitations200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.invitations)
                val invite = response.data!!.invitations.find { it.id == invitationIdCanceled }
                assertNotNull(invite, "Invitation $invitationIdCanceled not found")
                assertEquals(
                    ApplicationStatusDTO.CANCELED,
                    invite!!.status,
                    "Invitation status should be CANCELED",
                )
                assertEquals(
                    teamOwner.userId,
                    invite.processedBy?.id,
                    "ProcessedBy should be the owner who canceled",
                )
                assertNotNull(invite.processedAt)
            }
    }

    @Test
    @Order(125)
    fun `Invitation - Owner invites user again`() {
        invitationIdDeclined =
            inviteUser(teamOwnerToken, teamId, inviteeUser.userId, TeamMemberRoleTypeDTO.MEMBER)
                ?: fail("Failed to create second invitation")
        assertTrue(invitationIdDeclined > 0)
    }

    @Test
    @Order(130)
    fun `Invitation - Invitee declines the invitation`() {
        declineInvitation(inviteeToken, invitationIdDeclined)
    }

    @Test
    @Order(135)
    fun `Invitation - Invitee lists invitations (check declined)`() {
        webTestClient
            .get()
            .uri("/users/me/team-invitations")
            .header("Authorization", "Bearer $inviteeToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyInvitations200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.invitations)
                val invite = response.data!!.invitations.find { it.id == invitationIdDeclined }
                assertNotNull(invite, "Invitation $invitationIdDeclined not found")
                assertEquals(
                    ApplicationStatusDTO.DECLINED,
                    invite!!.status,
                    "Invitation status should be DECLINED",
                )
                assertEquals(
                    inviteeUser.userId,
                    invite.processedBy?.id,
                    "ProcessedBy should be the invitee who declined",
                )
                assertNotNull(invite.processedAt)
            }
    }

    @Test
    @Order(140)
    fun `Invitation - Owner invites user a third time as ADMIN`() {
        invitationIdAccepted =
            inviteUser(teamOwnerToken, teamId, inviteeUser.userId, TeamMemberRoleTypeDTO.ADMIN)
                ?: fail("Failed to create third invitation") // Invite as ADMIN this time
        assertTrue(invitationIdAccepted > 0)
    }

    @Test
    @Order(145)
    fun `Invitation - Invitee accepts the ADMIN invitation`() {
        acceptInvitation(inviteeToken, invitationIdAccepted)
    }

    @Test
    @Order(150)
    fun `Invitation - Verify user is now an ADMIN after acceptance`() {
        webTestClient
            .get()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $teamOwnerToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeamMembers200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.members)
                val newMember = response.data.members.find { it.user.id == inviteeUser.userId }
                assertNotNull(newMember, "User ${inviteeUser.userId} should now be a member")
                // Should join with ADMIN role as invited
                assertEquals(TeamMemberRoleTypeDTO.ADMIN, newMember!!.role)
            }

        // Verify from the user's perspective
        webTestClient
            .get()
            .uri("/teams/$teamId")
            .header("Authorization", "Bearer $inviteeToken") // Use invitee's token
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTeam200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.team)
                assertEquals(true, response.data.team.joined, "User should be marked as joined")
                assertEquals(
                    TeamMemberRoleTypeDTO.ADMIN,
                    response.data.team.role,
                    "User role should be ADMIN",
                )
            }
    }

    @Test
    @Order(155)
    fun `Invitation - Invitee lists invitations (check accepted)`() {
        webTestClient
            .get()
            .uri("/users/me/team-invitations")
            .header("Authorization", "Bearer $inviteeToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ListMyInvitations200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data?.invitations)
                val invite = response.data!!.invitations.find { it.id == invitationIdAccepted }
                assertNotNull(invite, "Invitation $invitationIdAccepted not found")
                assertEquals(
                    ApplicationStatusDTO.ACCEPTED,
                    invite!!.status,
                    "Invitation status should be ACCEPTED",
                )
                assertEquals(
                    inviteeUser.userId,
                    invite.processedBy?.id,
                    "ProcessedBy should be the invitee who accepted",
                )
                assertNotNull(invite.processedAt)
            }
    }

    // --- Error Handling / Edge Cases ---
    @Test
    @Order(200)
    fun `Request - Fails when user already member`() {
        // `requesterUser` is already a member of `teamId` from test 60
        requestToJoin(
                requesterToken,
                teamId,
                "Try joining again",
                expectedStatus = HttpStatus.CONFLICT,
            )
            .also { assertNull(it, "Request should fail, ID should be null") }

        // Verify with specific error DTO if possible
        webTestClient
            .post()
            .uri("/teams/$teamId/requests")
            .header("Authorization", "Bearer $requesterToken")
            .contentType(MediaType.APPLICATION_JSON) // Required even if body is implied empty
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody<GenericErrorResponse>()
            .value { error ->
                assertEquals("UserAlreadyMemberError", error.error.name)
            } // Check error name
    }

    @Test
    @Order(205)
    fun `Request - Fails when pending request exists`() {
        val tempRequestId =
            requestToJoin(anotherUserToken, teamId, "Initial Request for conflict test")
                ?: fail("Setup failed")

        // Try requesting again while PENDING
        requestToJoin(
                anotherUserToken,
                teamId,
                "Second Request",
                expectedStatus = HttpStatus.CONFLICT,
            )
            .also { assertNull(it, "Second request should fail") }

        // Verify error type
        webTestClient
            .post()
            .uri("/teams/$teamId/requests")
            .header("Authorization", "Bearer $anotherUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("PendingApplicationExistsError", error.error.name) }

        // Cleanup: Cancel the request
        cancelMyJoinRequest(anotherUserToken, tempRequestId)
    }

    @Test
    @Order(210)
    fun `Invitation - Fails when user already member`() {
        // `inviteeUser` is already an ADMIN of `teamId` from test 150
        inviteUser(
                teamOwnerToken,
                teamId,
                inviteeUser.userId,
                TeamMemberRoleTypeDTO.MEMBER,
                expectedStatus = HttpStatus.CONFLICT,
            )
            .also { assertNull(it, "Invitation should fail, ID should be null") }

        // Verify error type
        webTestClient
            .post()
            .uri("/teams/$teamId/invitations")
            .header("Authorization", "Bearer $teamOwnerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TeamInvitationCreateDTO(
                    userId = inviteeUser.userId,
                    role = TeamMemberRoleTypeDTO.MEMBER,
                )
            )
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("UserAlreadyMemberError", error.error.name) }
    }

    @Test
    @Order(215)
    fun `Invitation - Fails when pending invitation exists`() {
        val tempInviteId =
            inviteUser(teamOwnerToken, teamId, anotherUser.userId, TeamMemberRoleTypeDTO.MEMBER)
                ?: fail("Setup failed")

        // Try inviting again while PENDING
        inviteUser(
                teamOwnerToken,
                teamId,
                anotherUser.userId,
                TeamMemberRoleTypeDTO.ADMIN,
                expectedStatus = HttpStatus.CONFLICT,
            )
            .also { assertNull(it, "Second invitation should fail") }

        // Verify error type
        webTestClient
            .post()
            .uri("/teams/$teamId/invitations")
            .header("Authorization", "Bearer $teamOwnerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                TeamInvitationCreateDTO(
                    userId = anotherUser.userId,
                    role = TeamMemberRoleTypeDTO.ADMIN,
                )
            )
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody<GenericErrorResponse>()
            .value { error -> assertEquals("PendingApplicationExistsError", error.error.name) }

        // Cleanup: Cancel the invitation
        cancelInvitation(teamOwnerToken, teamId, tempInviteId)
    }
}
