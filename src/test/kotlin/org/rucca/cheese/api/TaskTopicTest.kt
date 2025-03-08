/*
 *  Description: It tests the feature of task topics.
 *
 *  Author(s):
 *      Nictheboy Li    <nictheboy@outlook.com>
 *
 */

package org.rucca.cheese.api

import kotlin.math.floor
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.client.TaskClient
import org.rucca.cheese.client.TopicClient
import org.rucca.cheese.client.UserClient
import org.rucca.cheese.common.persistent.IdType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class TaskTopicTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userClient: UserClient,
    private val topicClient: TopicClient,
    private val taskClient: TaskClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserClient.CreateUserResponse
    lateinit var creatorToken: String
    private var taskId: IdType = -1
    private val taskSubmissionSchema =
        listOf(Pair("Text Entry", "TEXT"), Pair("Attachment Entry", "FILE"))
    private val testTopicsCount = 4
    private val testTopics = mutableListOf<Pair<IdType, String>>()

    @BeforeAll
    fun prepare() {
        creator = userClient.createUser()
        creatorToken = userClient.login(creator.username, creator.password)
        for (i in 1..testTopicsCount) {
            val topicName = "Test Topic (${floor(Math.random() * 10000000000).toLong()}) ($i)"
            val topicId = topicClient.createTopic(creatorToken, topicName)
            testTopics.add(Pair(topicId, topicName))
        }
        taskId =
            taskClient.createTask(
                creatorToken,
                submissionSchema = taskSubmissionSchema,
                topics = listOf(testTopics[0].first, testTopics[1].first),
            )
    }

    @Test
    @Order(10)
    fun testGetTaskWithTopics() {
        val request =
            MockMvcRequestBuilders.get("/tasks/${taskId}")
                .queryParam("queryTopics", "true")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.task.topics.length()").value(2))
            .andExpect(
                jsonPath("$.data.task.topics[?(@.id == ${testTopics[0].first})].name")
                    .value(testTopics[0].second)
            )
            .andExpect(
                jsonPath("$.data.task.topics[?(@.id == ${testTopics[1].first})].name")
                    .value(testTopics[1].second)
            )
    }

    @Test
    @Order(20)
    fun testUpdateTaskTopics() {
        val request =
            MockMvcRequestBuilders.patch("/tasks/$taskId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "topics": [${testTopics[1].first}, ${testTopics[2].first}]
                }
            """
                )
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.task.topics.length()").value(2))
            .andExpect(
                jsonPath("$.data.task.topics[?(@.id == ${testTopics[1].first})].name")
                    .value(testTopics[1].second)
            )
            .andExpect(
                jsonPath("$.data.task.topics[?(@.id == ${testTopics[2].first})].name")
                    .value(testTopics[2].second)
            )
    }

    @Test
    @Order(30)
    fun testGetTaskWithTopics2() {
        val request =
            MockMvcRequestBuilders.get("/tasks/${taskId}")
                .queryParam("queryTopics", "true")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.task.topics.length()").value(2))
            .andExpect(
                jsonPath("$.data.task.topics[?(@.id == ${testTopics[1].first})].name")
                    .value(testTopics[1].second)
            )
            .andExpect(
                jsonPath("$.data.task.topics[?(@.id == ${testTopics[2].first})].name")
                    .value(testTopics[2].second)
            )
    }

    @Test
    @Order(40)
    fun testEnumerateTasksUseTopics1() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
                .param("topics", testTopics[0].first.toString())
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.length()").value(0))
    }

    @Test
    @Order(40)
    fun testEnumerateTasksUseTopics2() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
                .param("topics", testTopics[1].first.toString())
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.length()").value(1))
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskId))
    }

    @Test
    @Order(40)
    fun testEnumerateTasksUseTopics3() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
                .param("topics", testTopics[1].first.toString())
                .param("topics", testTopics[3].first.toString())
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.length()").value(1))
            .andExpect(jsonPath("$.data.tasks[0].id").value(taskId))
    }

    @Test
    @Order(40)
    fun testEnumerateTasksUseTopics4() {
        val request =
            MockMvcRequestBuilders.get("/tasks")
                .header("Authorization", "Bearer $creatorToken")
                .param("approved", "APPROVED")
                .param("topics", testTopics[0].first.toString())
                .param("topics", testTopics[3].first.toString())
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.tasks.length()").value(0))
    }
}
