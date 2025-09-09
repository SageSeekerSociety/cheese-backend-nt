package org.rucca.cheese.cli

import kotlin.random.Random
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.rucca.cheese.client.*
import org.rucca.cheese.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
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
        val member1Token = userClient.login(member1Response.username, member1Response.password)
        val member2Response = userClient.createUser()
        val member2Token = userClient.login(member2Response.username, member2Response.password)

        // Add members to the team (with their tokens to accept invitations)
        teamClient.addTeamMember(
            webTestClient = webTestClient,
            token = ownerToken,
            teamId = teamId,
            userId = member1Response.userId,
            userToken = member1Token,
        )

        teamClient.addTeamAdmin(
            webTestClient = webTestClient,
            creatorToken = ownerToken,
            teamId = teamId,
            adminId = member2Response.userId,
            adminToken = member2Token,
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
        val member1Token = userClient.login(member1Response.username, member1Response.password)
        val member2Response = userClient.createUser()
        val member2Token = userClient.login(member2Response.username, member2Response.password)

        // Create space
        val (spaceId, defaultCategoryId) =
            spaceClient.createSpace(webTestClient = webTestClient, creatorToken = ownerToken)

        // Create team
        val teamId = teamClient.createTeam(webTestClient = webTestClient, creatorToken = ownerToken)

        // Add members to team (with their tokens to accept invitations)
        teamClient.addTeamMember(
            webTestClient = webTestClient,
            token = ownerToken,
            teamId = teamId,
            userId = member1Response.userId,
            userToken = member1Token,
        )

        teamClient.addTeamAdmin(
            webTestClient = webTestClient,
            creatorToken = ownerToken,
            teamId = teamId,
            adminId = member2Response.userId,
            adminToken = member2Token,
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
        println("  Token: $ownerToken")
        println("Member 1:")
        println("  Username: ${member1Response.username}")
        println("  Password: ${member1Response.password}")
        println("  Token: $member1Token")
        println("Member 2:")
        println("  Username: ${member2Response.username}")
        println("  Password: ${member2Response.password}")
        println("  Token: $member2Token")
        println("========================================")
    }

    @Test
    @Commit
    fun createSpaceWithTasks() {
        // Create space owner
        val ownerResponse = userClient.createUser()
        val ownerToken = userClient.login(ownerResponse.username, ownerResponse.password)

        // Create space
        val (spaceId, defaultCategoryId) =
            spaceClient.createSpace(webTestClient = webTestClient, creatorToken = ownerToken)

        // Create additional categories
        val category2Id =
            spaceClient.createCategory(
                webTestClient = webTestClient,
                spaceId = spaceId,
                token = ownerToken,
                name = "Algorithm",
                description = "Algorithm problems",
            )

        val category3Id =
            spaceClient.createCategory(
                webTestClient = webTestClient,
                spaceId = spaceId,
                token = ownerToken,
                name = "Web Development",
                description = "Web development tasks",
            )

        // Create multiple tasks with different statuses
        val taskIds = mutableListOf<Long>()
        val participants = mutableListOf<Pair<String, String>>() // username, token pairs

        // Create 10 tasks
        for (i in 1..10) {
            val categoryId =
                when (i % 3) {
                    0 -> defaultCategoryId
                    1 -> category2Id
                    else -> category3Id
                }

            val taskId =
                taskClient.createTask(
                    webTestClient = webTestClient,
                    spaceId = spaceId,
                    categoryId = categoryId,
                    token = ownerToken,
                    name = "Task $i - ${getCategoryName(i % 3)}",
                    description = "This is task $i for testing analytics",
                    rank = i * 10, // 添加rank值：10, 20, 30...
                    maxParticipants = 50,
                    enableRanking = i % 2 == 0,
                )
            if (taskId != null) {
                // Approve the task so users can join
                taskClient.approveTask(webTestClient, taskId, ownerToken)
                taskIds.add(taskId)
                println("Created and approved task $taskId")
            } else {
                println("ERROR: Failed to create task $i in category $categoryId")
            }
        }

        // Create 20 participants
        for (i in 1..20) {
            val userResponse = userClient.createUser()
            val userToken = userClient.login(userResponse.username, userResponse.password)
            participants.add(Pair(userResponse.username, userToken))

            // Join random tasks (each user joins 2-5 tasks)
            val numTasksToJoin = Random.nextInt(2, 6)
            val tasksToJoin = taskIds.shuffled().take(numTasksToJoin)

            for (taskId in tasksToJoin) {
                // Join task (student info would need to be set via user identity endpoint)
                val participantId =
                    taskClient.joinTask(
                        webTestClient = webTestClient,
                        taskId = taskId,
                        token = userToken,
                        userId = userResponse.userId,
                    )

                // Update participant with real name info (simulate what users would do)
                // Note: In real system, this would be done via a separate API
                // For testing, we directly update the database to ensure complete data

                // Approve participant first (required before submission)
                taskClient.approveTaskParticipant(
                    webTestClient = webTestClient,
                    token = ownerToken,
                    taskId = taskId,
                    participantId = participantId,
                )

                // Submit task (70% chance)
                if (Random.nextDouble() < 0.7) {
                    try {
                        taskClient.submitTask(
                            webTestClient = webTestClient,
                            taskId = taskId,
                            token = userToken,
                            participantId = participantId,
                            content = "Submission for task $taskId by user $i",
                        )
                        println("User $i submitted to task $taskId")
                    } catch (e: Exception) {
                        println("Failed to submit for user $i to task $taskId: ${e.message}")
                    }
                }
            }
        }

        println("========================================")
        println("Space with Tasks created successfully!")
        println("========================================")
        println("SPACE ID: $spaceId")
        println("CATEGORIES:")
        println("  - General (ID: $defaultCategoryId)")
        println("  - Algorithm (ID: $category2Id)")
        println("  - Web Development (ID: $category3Id)")
        println("========================================")
        println("TASKS: ${taskIds.size} tasks created")
        println("PARTICIPANTS: ${participants.size} users created and joined tasks")
        println("========================================")
        println("Space Owner:")
        println("  Username: ${ownerResponse.username}")
        println("  Password: ${ownerResponse.password}")
        println("  Token: $ownerToken")
        println("========================================")
        println("Sample Participants (first 3):")
        participants.take(3).forEachIndexed { index, (username, token) ->
            println("Participant ${index + 1}:")
            println("  Username: $username")
            println("  Password: abc123456!!!")
            println("  Token: ${token.take(50)}...")
        }
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

    private fun getCategoryName(index: Int): String =
        when (index) {
            0 -> "General"
            1 -> "Algorithm"
            else -> "Web Development"
        }

    private fun getRandomGrade(): String =
        listOf("Freshman", "Sophomore", "Junior", "Senior").random()

    private fun getRandomMajor(): String =
        listOf(
                "Computer Science",
                "Software Engineering",
                "Information Technology",
                "Data Science",
                "Artificial Intelligence",
            )
            .random()
}
