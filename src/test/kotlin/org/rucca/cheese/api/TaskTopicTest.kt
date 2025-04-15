/*
 *  Description: It tests the feature of task topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.floor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.rucca.cheese.task.TaskSubmitterType
import org.rucca.cheese.task.toDTO
import org.rucca.cheese.utils.TopicCreatorService
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation::class)
class TaskTopicTest
@Autowired
constructor(
    private val webTestClient: WebTestClient,
    private val userCreatorService: UserCreatorService,
    private val topicCreatorService: TopicCreatorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    private var spaceId: IdType = -1
    private var defaultCategoryId: IdType = -1
    private var taskId: IdType = -1

    // --- Task Details ---
    private val randomSuffix = floor(Math.random() * 10000000000).toLong()
    private val taskName = "Test Task ($randomSuffix)"
    private val taskIntro = "This is a test task."
    private val taskDescription = "This is a test task."
    private val taskDeadline =
        LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val taskSubmissionSchema =
        listOf(
            TaskSubmissionSchemaEntryDTO("Text Entry", TaskSubmissionTypeDTO.TEXT),
            TaskSubmissionSchemaEntryDTO("Attachment Entry", TaskSubmissionTypeDTO.FILE),
        )

    // --- Topics ---
    private val testTopicsCount = 4

    // Store Pair<IdType, String> to easily verify name later
    private val testTopics = mutableListOf<Pair<IdType, String>>()

    // --- Refactored Helper Methods ---

    fun createSpace(
        creatorToken: String,
        spaceName: String,
        spaceIntro: String,
        spaceDescription: String,
        spaceAvatarId: IdType,
    ): Pair<IdType, IdType> {
        val requestDTO =
            PostSpaceRequestDTO(
                name = spaceName,
                intro = spaceIntro,
                description = spaceDescription,
                avatarId = spaceAvatarId,
                announcements = "[]",
                taskTemplates = "[]",
            )
        val response =
            webTestClient
                .post()
                .uri("/spaces")
                .header("Authorization", "Bearer $creatorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PatchSpace200ResponseDTO>() // Assuming POST returns Patch DTO
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.space, "Space data should not be null in response")
        val createdSpace = response!!.data.space
        assertNotNull(createdSpace.id, "Created space ID cannot be null")
        assertNotNull(createdSpace.defaultCategoryId, "Default category ID cannot be null")

        val createdSpaceId = createdSpace.id
        val createdDefaultCategoryId = createdSpace.defaultCategoryId
        logger.info(
            "Created space: $createdSpaceId with default category ID: $createdDefaultCategoryId"
        )
        return Pair(createdSpaceId, createdDefaultCategoryId)
    }

    fun createTask(
        name: String,
        submitterType: TaskSubmitterType, // Consider TaskSubmitterTypeDTO
        deadline: Long,
        resubmittable: Boolean,
        editable: Boolean,
        intro: String,
        description: String,
        submissionSchema: List<TaskSubmissionSchemaEntryDTO>, // Use data class
        space: IdType,
        categoryId: IdType?,
        topics: List<IdType>, // List of topic IDs for request
    ): IdType {
        val requestDTO =
            PostTaskRequestDTO(
                name = name,
                submitterType = submitterType.toDTO(),
                deadline = deadline,
                resubmittable = resubmittable,
                editable = editable,
                intro = intro,
                description = description,
                submissionSchema = submissionSchema,
                space = space,
                categoryId = categoryId,
                topics = topics, // Pass the list of topic IDs
                rank = null, // Add defaults if needed
                defaultDeadline = 30L, // Add defaults if needed
            )

        val response =
            webTestClient
                .post()
                .uri("/tasks")
                .header(
                    "Authorization",
                    "Bearer $creatorToken",
                ) // Assumes creatorToken always used here
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<PatchTask200ResponseDTO>() // Assuming POST returns this DTO
                .returnResult()
                .responseBody

        assertNotNull(response?.data?.task, "Task data should not be null")
        val task = response!!.data.task
        assertNotNull(task.id, "Created task ID should not be null")

        // --- Assertions for topics in response DTO ---
        assertNotNull(task.topics, "Topics list should exist in response task DTO")
        assertEquals(topics.size, task.topics!!.size, "Topic count mismatch in response")
        val returnedTopicIds = task.topics!!.map { it.id }.toSet()
        topics.forEach { expectedTopicId ->
            assertTrue(
                returnedTopicIds.contains(expectedTopicId),
                "Expected topic ID $expectedTopicId not found in response",
            )
        }
        // Ensure names are present (basic check)
        assertTrue(
            task.topics!!.all { it.name.isNotBlank() },
            "All returned topics should have names",
        )

        val createdTaskId = task.id
        logger.info("Created task: $createdTaskId")
        return createdTaskId
    }

    fun approveTask(taskId: IdType, token: String) {
        val requestDTO = PatchTaskRequestDTO(approved = ApproveTypeDTO.APPROVED)
        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task)
                assertEquals(ApproveTypeDTO.APPROVED, response.data.task.approved)
            }
    }

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)

        // Create topics and store ID/Name pairs
        for (i in 1..testTopicsCount) {
            val topicName = "Test Topic ($randomSuffix) ($i)"
            // Assuming TopicCreatorService uses WebTestClient or similar internally now
            val topicId = topicCreatorService.createTopic(creatorToken, topicName)
            testTopics.add(Pair(topicId, topicName))
        }
        assertTrue(testTopics.size == testTopicsCount)
        logger.info("Created test topics: $testTopics")

        // Create space
        val spaceResult =
            createSpace(
                creatorToken = creatorToken,
                spaceName = "Test Space ($randomSuffix)",
                spaceIntro = "This is a test space.",
                spaceDescription = "A lengthy text. ".repeat(100),
                spaceAvatarId = userCreatorService.testAvatarId(),
            )
        spaceId = spaceResult.first
        defaultCategoryId = spaceResult.second

        // Create initial task with first two topics
        taskId =
            createTask(
                name = "$taskName (Initial)", // Adjusted name
                submitterType = TaskSubmitterType.USER,
                deadline = taskDeadline,
                resubmittable = true,
                editable = true,
                intro = taskIntro,
                description = taskDescription,
                submissionSchema = taskSubmissionSchema,
                space = spaceId,
                categoryId = defaultCategoryId,
                topics = listOf(testTopics[0].first, testTopics[1].first), // Use first two topic IDs
            )
        assertTrue(taskId > 0)

        // Approve the task
        approveTask(taskId, creatorToken)
    }

    // Helper to assert topics in GetTask response
    private fun assertTaskTopics(taskDto: TaskDTO?, expectedTopics: List<Pair<IdType, String>>) {
        assertNotNull(taskDto, "Task DTO is null")
        assertNotNull(taskDto!!.topics, "Task topics list is null")
        val actualTopics = taskDto.topics!!
        assertEquals(expectedTopics.size, actualTopics.size, "Topic count mismatch")
        val actualTopicMap = actualTopics.associateBy { it.id }
        expectedTopics.forEach { (expectedId, expectedName) ->
            assertTrue(
                actualTopicMap.containsKey(expectedId),
                "Expected topic ID $expectedId not found",
            )
            assertEquals(
                expectedName,
                actualTopicMap[expectedId]?.name,
                "Name mismatch for topic ID $expectedId",
            )
        }
    }

    @Test
    @Order(10)
    fun `Task - Get task with initial topics`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/tasks/$taskId").queryParam("queryTopics", "true").build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task, "Task data missing")
                // Assert the initial two topics are present
                assertTaskTopics(response.data.task, listOf(testTopics[0], testTopics[1]))
            }
    }

    @Test
    @Order(20)
    fun `Task - Update task topics`() { // Renamed
        val updatedTopicIds =
            listOf(
                testTopics[1].first,
                testTopics[2].first,
            ) // Keep topic 1, replace topic 0 with topic 2
        // Assuming PatchTaskRequestDTO exists
        val requestDTO = PatchTaskRequestDTO(topics = updatedTopicIds)

        webTestClient
            .patch()
            .uri("/tasks/$taskId")
            .header("Authorization", "Bearer $creatorToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestDTO)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<PatchTask200ResponseDTO>() // Assuming patch returns updated task
            .value { response ->
                assertNotNull(response.data.task, "Task data missing")
                // Assert the updated topics (ID and Name)
                assertTaskTopics(response.data.task, listOf(testTopics[1], testTopics[2]))
            }
    }

    @Test
    @Order(30)
    fun `Task - Get task with updated topics`() { // Renamed
        webTestClient
            .get()
            .uri { builder ->
                builder.path("/tasks/$taskId").queryParam("queryTopics", "true").build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTask200ResponseDTO>()
            .value { response ->
                assertNotNull(response.data.task, "Task data missing")
                // Assert the topics reflect the update from test 20
                assertTaskTopics(response.data.task, listOf(testTopics[1], testTopics[2]))
            }
    }

    // Helper for enumerating tasks by topic
    private fun assertEnumerateTasksByTopics(
        expectedTaskIds: Set<IdType>,
        vararg topicIdsToFilter: IdType,
    ) {
        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path("/tasks")
                    .queryParam("space", spaceId)
                    .queryParam("approved", ApproveTypeDTO.APPROVED.name)
                // Add each topic ID as a separate query parameter
                topicIdsToFilter.forEach { builder.queryParam("topics", it) }
                builder.build()
            }
            .header("Authorization", "Bearer $creatorToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<GetTasks200ResponseDTO>() // Assuming enumeration uses this DTO
            .value { response ->
                assertNotNull(response.data?.tasks, "Tasks list missing in response")
                val actualTaskIds = response.data!!.tasks!!.map { it.id }.toSet()
                assertEquals(
                    expectedTaskIds,
                    actualTaskIds,
                    "Filtered task ID set mismatch for topics ${topicIdsToFilter.joinToString()}",
                )
            }
    }

    @Test
    @Order(40) // Grouped original tests into sub-assertions
    fun `Task - Enumerate tasks filtered by topics`() { // Renamed
        // Current state: Task 'taskId' has topics 1 and 2 (indices 1 and 2 in testTopics)

        // Filter by topic 0 (index 0) - Task no longer has it -> Expect 0 tasks
        assertEnumerateTasksByTopics(emptySet(), testTopics[0].first)

        // Filter by topic 1 (index 1) - Task has it -> Expect 1 task (taskId)
        assertEnumerateTasksByTopics(setOf(taskId), testTopics[1].first)

        // Filter by topic 1 OR topic 3 (indices 1, 3) - Task has topic 1 -> Expect 1 task (taskId)
        assertEnumerateTasksByTopics(setOf(taskId), testTopics[1].first, testTopics[3].first)

        // Filter by topic 0 OR topic 3 (indices 0, 3) - Task has neither -> Expect 0 tasks
        assertEnumerateTasksByTopics(emptySet(), testTopics[0].first, testTopics[3].first)
    }
}
