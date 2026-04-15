package org.rucca.cheese.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.rucca.cheese.client.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@EnabledIfSystemProperty(named = "cli", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class QuickSubmissionTest
@Autowired
constructor(
    private val userClient: UserClient,
    private val spaceClient: SpaceClient,
    private val taskClient: TaskClient,
    private val webTestClient: WebTestClient,
) {

    @Test
    @Commit
    fun createTaskWithSubmission() {
        // Create owner
        val ownerResponse = userClient.createUser()
        val ownerToken = userClient.login(ownerResponse.username, ownerResponse.password)

        // Create participant
        val participantResponse = userClient.createUser()
        val participantToken =
            userClient.login(participantResponse.username, participantResponse.password)

        // Create space
        val (spaceId, categoryId) =
            spaceClient.createSpace(webTestClient = webTestClient, creatorToken = ownerToken)

        // Create task
        val taskId =
            taskClient.createTask(
                webTestClient = webTestClient,
                spaceId = spaceId,
                categoryId = categoryId,
                token = ownerToken,
                name = "Test Task with Submission",
            )

        if (taskId != null) {
            // Approve task
            taskClient.approveTask(webTestClient, taskId, ownerToken)

            // Join task as participant
            val participantId =
                taskClient.joinTask(
                    webTestClient = webTestClient,
                    taskId = taskId,
                    token = participantToken,
                    userId = participantResponse.userId,
                )

            // Approve participant
            taskClient.approveTaskParticipant(
                webTestClient = webTestClient,
                token = ownerToken,
                taskId = taskId,
                participantId = participantId,
            )

            // Submit task
            try {
                val submissionId =
                    taskClient.submitTask(
                        webTestClient = webTestClient,
                        taskId = taskId,
                        token = participantToken,
                        participantId = participantId,
                        content = "Test submission content",
                    )

                println("========================================")
                println("Task with Submission created successfully!")
                println("========================================")
                println("Space ID: $spaceId")
                println("Task ID: $taskId")
                println("Participant ID: $participantId")
                println("Submission ID: $submissionId")
                println("========================================")
                println("Owner:")
                println("  Username: ${ownerResponse.username}")
                println("  Password: ${ownerResponse.password}")
                println("Participant:")
                println("  Username: ${participantResponse.username}")
                println("  Password: ${participantResponse.password}")
                println("========================================")
            } catch (e: Exception) {
                println("Failed to create submission: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
