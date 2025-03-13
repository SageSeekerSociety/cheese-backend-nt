package org.rucca.cheese.api

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.floor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.UserCreatorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
@TestInstance(Lifecycle.PER_CLASS)
class KnowledgeTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
    private val objectMapper: ObjectMapper,
) {

    private lateinit var creatorToken: String
    private var knowledgeId: IdType = -1
    private val projectIds = listOf<Long>()
    private val labels = listOf("test", "demo")

    @BeforeAll
    fun setup() {
        val creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
    }

    @Test
    @Order(1)
    fun testCreateKnowledge() {
        val requestBody =
            mapOf(
                "name" to "Test Knowledge ${floor(Math.random() * 1000000).toInt()}",
                "type" to "DOCUMENT",
                "content" to
                    Json.encodeToString(mapOf("text" to "This is a test knowledge content.")),
                "description" to "A test knowledge description.",
                "projectIds" to projectIds,
                "labels" to labels,
            )
        val result =
            mockMvc
                .perform(
                    post("/knowledges")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.knowledge.id").exists())
                .andReturn()

        val responseJson = objectMapper.readTree(result.response.contentAsString)
        knowledgeId = responseJson.path("data").path("knowledge").path("id").asLong()
        // println("Created knowledge with ID: $knowledgeId")
        // println("Response JSON: ${result.response.contentAsString}")
    }

    @Test
    @Order(2)
    fun testGetKnowledgeSuccess() {
        println("Getting knowledge with ID: $knowledgeId")
        val result =
            mockMvc
                .perform(
                    get("/knowledges/$knowledgeId").header("Authorization", "Bearer $creatorToken")
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(knowledgeId))
                .andReturn()
        //   println("Get response: ${result.response.contentAsString}")
    }

    @Test
    @Order(3)
    fun testGetKnowledgeNotFound() {
        mockMvc
            .perform(get("/knowledges/-1").header("Authorization", "Bearer $creatorToken"))
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(4)
    fun testUpdateKnowledgeSuccess() {
        println("Before update - knowledgeId: $knowledgeId")
        if (knowledgeId == -1L) {
            throw IllegalStateException(
                "knowledgeId is not set. Please run testCreateKnowledge first."
            )
        }

        val updatedContent = Json.encodeToString(mapOf("text" to "Updated content"))
        val updateRequest =
            mapOf(
                "name" to "Updated Knowledge",
                "description" to "Updated description",
                "content" to updatedContent,
                "projectIds" to listOf(3L, 4L),
                "labels" to listOf("updated", "test2"),
            )

        val result =
            mockMvc
                .perform(
                    patch("/knowledges/$knowledgeId")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.knowledge.id").value(knowledgeId))
                .andExpect(jsonPath("$.data.knowledge.name").value("Updated Knowledge"))
                .andExpect(jsonPath("$.data.knowledge.description").value("Updated description"))
                .andExpect(jsonPath("$.data.knowledge.content").value(updatedContent))
                .andReturn()

        // println("After update - knowledgeId: $knowledgeId")
        // println("Update response: ${result.response.contentAsString}")
    }

    @Test
    @Order(5)
    fun testDeleteKnowledgeSuccess() {
        mockMvc
            .perform(
                delete("/knowledges/$knowledgeId").header("Authorization", "Bearer $creatorToken")
            )
            .andExpect(status().isOk)
    }
}
