package org.rucca.cheese.api

import kotlin.math.floor
import org.json.JSONObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.UserCreatorService
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
class KnowledgeTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    lateinit var admin: UserCreatorService.CreateUserResponse
    lateinit var adminToken: String
    lateinit var anonymous: UserCreatorService.CreateUserResponse
    lateinit var anonymousToken: String

    private var knowledgeName = "Test Knowledge (${floor(Math.random() * 10000000000).toLong()})"
    private var knowledgeContent = "This is test content."
    private var knowledgeDescription = "A lengthy description. ".repeat(100)
    private var knowledgeType = "document"
    private var knowledgeId: IdType = -1
    private var projectIds = listOf(1L, 2L)
    private var labels = listOf("test", "demo")

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
        admin = userCreatorService.createUser()
        adminToken = userCreatorService.login(admin.username, admin.password)
        anonymous = userCreatorService.createUser()
        anonymousToken = userCreatorService.login(anonymous.username, anonymous.password)
    }

    fun createKnowledge(
        token: String,
        name: String,
        type: String,
        content: String,
        description: String? = null,
        projectIds: List<Long>? = null,
        labels: List<String>? = null,
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/knowledge")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                    {
                        "name": "$name",
                        "type": "$type",
                        "content": "$content",
                        "description": ${if (description != null) "\"$description\"" else "null"},
                        "projectIds": ${projectIds?.let { JSONObject().put("projectIds", it).toString() } ?: "[]"},
                        "labels": ${labels?.let { JSONObject().put("labels", it).toString() } ?: "[]"}
                    }
                    """
                )
        val response = mockMvc.perform(request).andExpect(status().isOk)
        val knowledgeId =
            JSONObject(response.andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("knowledge")
                .getLong("id")
        logger.info("Created knowledge: $knowledgeId")
        return knowledgeId
    }

    @Test
    @Order(10)
    fun testGetKnowledgeAndNotFound() {
        val request =
            MockMvcRequestBuilders.get("/knowledge/-1")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.name").value("NotFoundError"))
            .andExpect(jsonPath("$.error.data.type").value("knowledge"))
            .andExpect(jsonPath("$.error.data.id").value("-1"))
    }

    @Test
    @Order(20)
    fun testCreateKnowledge() {
        knowledgeId =
            createKnowledge(
                creatorToken,
                knowledgeName,
                knowledgeType,
                knowledgeContent,
                knowledgeDescription,
                projectIds,
                labels,
            )
    }

    @Test
    @Order(30)
    fun testGetKnowledge() {
        val request =
            MockMvcRequestBuilders.get("/knowledge/$knowledgeId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.knowledge.id").value(knowledgeId))
            .andExpect(jsonPath("$.data.knowledge.name").value(knowledgeName))
            .andExpect(jsonPath("$.data.knowledge.type").value(knowledgeType))
            .andExpect(jsonPath("$.data.knowledge.content").value(knowledgeContent))
            .andExpect(jsonPath("$.data.knowledge.description").value(knowledgeDescription))
            .andExpect(jsonPath("$.data.knowledge.projectIds").isArray)
            .andExpect(jsonPath("$.data.knowledge.projectIds[0]").value(projectIds[0]))
            .andExpect(jsonPath("$.data.knowledge.projectIds[1]").value(projectIds[1]))
            .andExpect(jsonPath("$.data.knowledge.labels").isArray)
            .andExpect(jsonPath("$.data.knowledge.labels[0]").value(labels[0]))
            .andExpect(jsonPath("$.data.knowledge.labels[1]").value(labels[1]))
            .andExpect(jsonPath("$.data.knowledge.creator.id").value(creator.userId))
    }

    @Test
    @Order(40)
    fun testUpdateKnowledge() {
        val updatedName = "Updated Knowledge"
        val updatedDescription = "Updated description"
        val updatedContent = "Updated content"
        val updatedProjectIds = listOf(3L, 4L)
        val updatedLabels = listOf("updated", "test2")

        val request =
            MockMvcRequestBuilders.patch("/knowledge/$knowledgeId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                    {
                        "name": "$updatedName",
                        "description": "$updatedDescription",
                        "content": "$updatedContent",
                        "projectIds": ${JSONObject().put("projectIds", updatedProjectIds)},
                        "labels": ${JSONObject().put("labels", updatedLabels)}
                    }
                    """
                )

        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.knowledge.id").value(knowledgeId))
            .andExpect(jsonPath("$.data.knowledge.name").value(updatedName))
            .andExpect(jsonPath("$.data.knowledge.content").value(updatedContent))
            .andExpect(jsonPath("$.data.knowledge.description").value(updatedDescription))
            .andExpect(jsonPath("$.data.knowledge.projectIds[0]").value(updatedProjectIds[0]))
            .andExpect(jsonPath("$.data.knowledge.projectIds[1]").value(updatedProjectIds[1]))
            .andExpect(jsonPath("$.data.knowledge.labels[0]").value(updatedLabels[0]))
            .andExpect(jsonPath("$.data.knowledge.labels[1]").value(updatedLabels[1]))
    }

    @Test
    @Order(50)
    fun testPartialUpdateKnowledge() {
        val updatedName = "Partially Updated Knowledge"

        val request =
            MockMvcRequestBuilders.patch("/knowledge/$knowledgeId")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                    {
                        "name": "$updatedName"
                    }
                    """
                )

        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.knowledge.id").value(knowledgeId))
            .andExpect(jsonPath("$.data.knowledge.name").value(updatedName))
    }

    @Test
    @Order(60)
    fun testUpdateKnowledgeWithAnonymous() {
        val request =
            MockMvcRequestBuilders.patch("/knowledge/$knowledgeId")
                .header("Authorization", "Bearer $anonymousToken")
                .contentType("application/json")
                .content("{}")
        mockMvc
            .perform(request)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("PermissionDeniedError"))
    }

    @Test
    @Order(70)
    fun testListKnowledge() {
        val request =
            MockMvcRequestBuilders.get("/knowledge")
                .param("project_ids", projectIds[0].toString())
                .header("Authorization", "Bearer $creatorToken")
        mockMvc
            .perform(request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.knowledge").isArray)
            .andExpect(jsonPath("$.data.knowledge[0].id").exists())
            .andExpect(jsonPath("$.data.knowledge[0].name").exists())
            .andExpect(jsonPath("$.data.knowledge[0].type").exists())
            .andExpect(jsonPath("$.data.knowledge[0].content").exists())
    }

    @Test
    @Order(80)
    fun testDeleteKnowledge() {
        val request =
            MockMvcRequestBuilders.delete("/knowledge/$knowledgeId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(request).andExpect(status().isOk)

        // Verify the knowledge is deleted
        val getRequest =
            MockMvcRequestBuilders.get("/knowledge/$knowledgeId")
                .header("Authorization", "Bearer $creatorToken")
        mockMvc.perform(getRequest).andExpect(status().isNotFound)
    }

    @Test
    @Order(90)
    fun testDeleteKnowledgeWithAnonymous() {
        val newKnowledgeId =
            createKnowledge(
                creatorToken,
                "Knowledge to delete",
                knowledgeType,
                "Content",
                null,
                null,
                null,
            )

        val request =
            MockMvcRequestBuilders.delete("/knowledge/$newKnowledgeId")
                .header("Authorization", "Bearer $anonymousToken")
        mockMvc
            .perform(request)
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.name").value("PermissionDeniedError"))
    }
}
