package org.rucca.cheese.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.rucca.cheese.client.*
import org.rucca.cheese.model.TeamMemberRoleTypeDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * CLI for creating test data.
 *
 * Usage: ./mvnw test -Dtest=CliTest#createUser -Dcli=true -Dspotless.check.skip=true ./mvnw test
 * -Dtest=CliTest#createMultipleUsers -Dcli=true -Dcli.count=5 -Dspotless.check.skip=true ./mvnw
 * test -Dtest=CliTest#createFullEnvironment -Dcli=true -Dspotless.check.skip=true
 */
@EnabledIfSystemProperty(named = "cli", matches = "true")
@SpringBootTest
@ActiveProfiles("dev") // Use dev database by default
class CliTest
@Autowired
constructor(
    private val userClient: UserClient,
    private val spaceClient: SpaceClient,
    private val teamClient: TeamClient,
    private val taskClient: TaskClient,
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

        val spaceResponse = spaceClient.createSpace(token = token)
        val categoryResponse = spaceClient.createCategory(spaceId = spaceResponse.id, token = token)

        println("========================================")
        println("Space created successfully!")
        println("========================================")
        println("Space ID: ${spaceResponse.id}")
        println("Space Name: ${spaceResponse.name}")
        println("Category ID: ${categoryResponse.id}")
        println("Category Name: ${categoryResponse.name}")
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

        val teamResponse = teamClient.createTeam(token = ownerToken)

        val member1Response = userClient.createUser()
        val member2Response = userClient.createUser()

        teamClient.addMember(
            teamId = teamResponse.id,
            userId = member1Response.userId,
            role = TeamMemberRoleTypeDTO.member,
            token = ownerToken,
        )

        teamClient.addMember(
            teamId = teamResponse.id,
            userId = member2Response.userId,
            role = TeamMemberRoleTypeDTO.admin,
            token = ownerToken,
        )

        println("========================================")
        println("Team created successfully!")
        println("========================================")
        println("Team ID: ${teamResponse.id}")
        println("Team Name: ${teamResponse.name}")
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
        val spaceResponse = spaceClient.createSpace(token = ownerToken)
        val categoryResponse =
            spaceClient.createCategory(spaceId = spaceResponse.id, token = ownerToken)

        // Create team
        val teamResponse = teamClient.createTeam(token = ownerToken)
        teamClient.addMember(
            teamId = teamResponse.id,
            userId = member1Response.userId,
            role = TeamMemberRoleTypeDTO.member,
            token = ownerToken,
        )
        teamClient.addMember(
            teamId = teamResponse.id,
            userId = member2Response.userId,
            role = TeamMemberRoleTypeDTO.admin,
            token = ownerToken,
        )

        // Create task
        val taskResponse =
            taskClient.createTask(
                spaceId = spaceResponse.id,
                categoryId = categoryResponse.id,
                token = ownerToken,
            )

        println("========================================")
        println("Full test environment created!")
        println("========================================")
        println("SPACE:")
        println("  ID: ${spaceResponse.id}")
        println("  Name: ${spaceResponse.name}")
        println("  Category ID: ${categoryResponse.id}")
        println("========================================")
        println("TEAM:")
        println("  ID: ${teamResponse.id}")
        println("  Name: ${teamResponse.name}")
        println("========================================")
        println("TASK:")
        println("  ID: ${taskResponse.id}")
        println("  Title: ${taskResponse.title}")
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
