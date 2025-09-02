package org.rucca.cheese.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.rucca.cheese.client.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * CLI for creating test data.
 *
 * Usage: ./mvnw test -Dtest=CliTest#createUser -Dcli=true -Dspotless.check.skip=true ./mvnw test
 * -Dtest=CliTest#createMultipleUsers -Dcli=true -Dcli.count=5 -Dspotless.check.skip=true ./mvnw
 * test -Dtest=CliTest#createFullEnvironment -Dcli=true -Dspotless.check.skip=true
 */
@EnabledIfSystemProperty(named = "cli", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev") // Use dev database by default
class CliTest
@Autowired
constructor(
    private val userClient: UserClient,
    private val spaceClient: SpaceClient,
    private val teamClient: TeamClient,
    private val taskClient: TaskClient,
    private val webTestClient: WebTestClient,
) {

    @Test
    fun createUser() {
        val response = userClient.createUser()
        val token = userClient.login(response.username, response.password)

        println("========================================")
        println("User created successfully!")
        println("========================================")
        println("User ID: ${response.userId}")
        println("Username: ${response.username}")
        println("Password: ${response.password}")
        println("Email: ${response.email}")
        println("========================================")
        println("JWT Token:")
        println(token)
        println("========================================")
    }

    @Test
    fun createMultipleUsers() {
        val count = System.getProperty("cli.count", "3").toInt()

        println("========================================")
        println("Creating $count users...")
        println("========================================")

        for (i in 1..count) {
            val response = userClient.createUser()
            val token = userClient.login(response.username, response.password)

            println("User $i:")
            println("  ID: ${response.userId}")
            println("  Username: ${response.username}")
            println("  Password: ${response.password}")
            println("  Token: ${token.take(20)}...")
            println()
        }

        println("========================================")
        println("Created $count users successfully!")
        println("========================================")
    }

    @Test
    fun createSpace() {
        val userResponse = userClient.createUser()
        val token = userClient.login(userResponse.username, userResponse.password)

        val (spaceId, defaultCategoryId) =
            spaceClient.createSpace(webTestClient = webTestClient, creatorToken = token)

        println("========================================")
        println("Space created successfully!")
        println("========================================")
        println("Space ID: $spaceId")
        println("Default Category ID: $defaultCategoryId")
        println("========================================")
        println("Owner Credentials:")
        println("  Username: ${userResponse.username}")
        println("  Password: ${userResponse.password}")
        println("========================================")
    }

    @Test
    fun createTeam() {
        val ownerResponse = userClient.createUser()
        val ownerToken = userClient.login(ownerResponse.username, ownerResponse.password)

        val teamId = teamClient.createTeam(webTestClient = webTestClient, creatorToken = ownerToken)

        val member1Response = userClient.createUser()
        val member2Response = userClient.createUser()

        // Add members to the team
        teamClient.addTeamMember(
            webTestClient = webTestClient,
            token = ownerToken,
            teamId = teamId,
            userId = member1Response.userId
        )
        
        teamClient.addTeamAdmin(
            webTestClient = webTestClient,
            creatorToken = ownerToken,
            teamId = teamId,
            adminId = member2Response.userId
        )

        println("========================================")
        println("Team created successfully!")
        println("========================================")
        println("Team ID: $teamId")
        println("========================================")
        println("Owner:")
        println("  Username: ${ownerResponse.username}")
        println("  Password: ${ownerResponse.password}")
        println("Member 1 (member role):")
        println("  Username: ${member1Response.username}")
        println("  Password: ${member1Response.password}")
        println("Member 2 (admin role):")
        println("  Username: ${member2Response.username}")
        println("  Password: ${member2Response.password}")
        println("========================================")
    }

    @Test
    fun createFullEnvironment() {
        // Create users
        val ownerResponse = userClient.createUser()
        val ownerToken = userClient.login(ownerResponse.username, ownerResponse.password)

        val member1Response = userClient.createUser()
        val member2Response = userClient.createUser()

        // Create space
        val (spaceId, defaultCategoryId) =
            spaceClient.createSpace(webTestClient = webTestClient, creatorToken = ownerToken)

        // Create team
        val teamId = teamClient.createTeam(webTestClient = webTestClient, creatorToken = ownerToken)
        
        // Add members to team
        teamClient.addTeamMember(
            webTestClient = webTestClient,
            token = ownerToken,
            teamId = teamId,
            userId = member1Response.userId
        )
        
        teamClient.addTeamAdmin(
            webTestClient = webTestClient,
            creatorToken = ownerToken,
            teamId = teamId,
            adminId = member2Response.userId
        )

        // Create task
        val taskId =
            taskClient.createTask(
                webTestClient = webTestClient,
                spaceId = spaceId,
                categoryId = defaultCategoryId,
                token = ownerToken,
            )

        println("========================================")
        println("Full test environment created!")
        println("========================================")
        println("SPACE:")
        println("  ID: $spaceId")
        println("  Default Category ID: $defaultCategoryId")
        println("========================================")
        println("TEAM:")
        println("  ID: $teamId")
        println("========================================")
        println("TASK:")
        println("  ID: $taskId")
        println("========================================")
        println("USERS:")
        println("Owner:")
        println("  Username: ${ownerResponse.username}")
        println("  Password: ${ownerResponse.password}")
        println("  Token: ${ownerToken.take(50)}...")
        println("Member 1:")
        println("  Username: ${member1Response.username}")
        println("  Password: ${member1Response.password}")
        println("Member 2:")
        println("  Username: ${member2Response.username}")
        println("  Password: ${member2Response.password}")
        println("========================================")
    }

    @Test
    fun loginWithCredentials() {
        val username =
            System.getProperty("cli.username")
                ?: throw IllegalArgumentException("Please provide username with -Dcli.username=xxx")
        val password =
            System.getProperty("cli.password")
                ?: throw IllegalArgumentException("Please provide password with -Dcli.password=xxx")

        val token = userClient.login(username, password)

        println("========================================")
        println("Login successful!")
        println("========================================")
        println("JWT Token:")
        println(token)
        println("========================================")
    }
}
