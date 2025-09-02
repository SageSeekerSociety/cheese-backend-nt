package org.rucca.cheese.client

import kotlin.math.floor
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Client for creating and managing test teams. Provides reusable methods for team-related
 * operations in tests.
 */
@Service
class TeamClient(private val userClient: UserClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun testTeamName(): String {
        return "Test Team (${floor(Math.random() * 10000000000).toLong()})"
    }

    fun testTeamIntro(): String {
        return "This is a test team"
    }

    fun testTeamDescription(): String {
        return "A lengthy text. ".repeat(100)
    }

    /** Creates a team and returns its ID. */
    fun createTeam(
        webTestClient: WebTestClient,
        creatorToken: String,
        teamName: String = testTeamName(),
        teamIntro: String = testTeamIntro(),
        teamDescription: String = testTeamDescription(),
        teamAvatarId: IdType = userClient.testAvatarId(),
    ): IdType {
        val requestDTO =
            PostTeamRequestDTO(
                name = teamName,
                intro = teamIntro,
                description = teamDescription,
                avatarId = teamAvatarId,
            )

        val responseDTO =
            webTestClient
                .post()
                .uri("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<GetTeam200ResponseDTO>()
                .returnResult()
                .responseBody!!

        val teamId = responseDTO.data.team.id
        logger.info("Created team: $teamId")
        return teamId
    }

    /** Adds a member to a team. */
    fun addTeamMember(
        webTestClient: WebTestClient,
        token: String,
        teamId: IdType,
        userId: IdType,
        role: TeamMemberRoleTypeDTO = TeamMemberRoleTypeDTO.MEMBER,
    ) {
        val requestDTO = PostTeamMemberRequestDTO(role = role, userId = userId)

        webTestClient
            .post()
            .uri("/teams/$teamId/members")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk()
    }

    /** Adds an admin to a team. */
    fun addTeamAdmin(
        webTestClient: WebTestClient,
        creatorToken: String,
        teamId: IdType,
        adminId: IdType,
    ) {
        addTeamMember(webTestClient, creatorToken, teamId, adminId, TeamMemberRoleTypeDTO.ADMIN)
    }
}
