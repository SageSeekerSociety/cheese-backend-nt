package org.rucca.cheese.api

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.floor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.team.Team
import org.rucca.cheese.team.TeamMemberRole
import org.rucca.cheese.team.TeamRepository
import org.rucca.cheese.team.TeamUserRelation
import org.rucca.cheese.team.TeamUserRelationRepository
import org.rucca.cheese.user.AvatarRepository
import org.rucca.cheese.user.UserRepository
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
    private val teamRepository: TeamRepository,
    private val teamUserRelationRepository: TeamUserRelationRepository,
    private val objectMapper: ObjectMapper,
    private val avatarRepository: AvatarRepository,
) {
    companion object {
        const val DEFAULT_AVATAR = 1
    }

    @Autowired private lateinit var userRepository: UserRepository
    private lateinit var creatorToken: String
    private var knowledgeId: IdType = -1
    private var teamId: IdType = -1
    private var userId: IdType = -1
    private val projectId: Long? = null
    private val labels = listOf("test", "demo")
    private val discussionId: Long? = null

    @BeforeAll
    fun setup() {
        // 创建用户
        val creator = userCreatorService.createUser()
        userId = creator.userId
        creatorToken = userCreatorService.login(creator.username, creator.password)

        // 创建团队
        val team =
            Team(
                name = "Test Team ${floor(Math.random() * 10000).toInt()}",
                intro = "Test team intro",
                description = "Test team description",
                avatar = avatarRepository.getReferenceById(DEFAULT_AVATAR),
            )
        val savedTeam = teamRepository.save(team)
        teamId = savedTeam.id!!.toLong()

        // 将用户添加到团队
        val teamUserRelation =
            TeamUserRelation(
                user = userRepository.getReferenceById(creator.userId.toInt()),
                team = savedTeam,
                role = TeamMemberRole.OWNER,
            )
        teamUserRelationRepository.save(teamUserRelation)
    }

    @Test
    @Order(1)
    fun testCreateKnowledge() {
        val requestBody =
            mapOf(
                "name" to "Test Knowledge ${floor(Math.random() * 1000000).toInt()}",
                "description" to "A test knowledge description.",
                "type" to "TEXT",
                "content" to
                    Json.encodeToString(mapOf("text" to "This is a test knowledge content.")),
                "teamId" to teamId,
                "projectId" to projectId,
                "discussionId" to discussionId,
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
                .andExpect(jsonPath("$.data.teamId").value(teamId))
                .andReturn()
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
    fun testGetKnowledgesByTeam() {
        mockMvc
            .perform(
                get("/knowledges")
                    .param("teamId", teamId.toString())
                    .header("Authorization", "Bearer $creatorToken")
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.knowledges").isArray)
            .andExpect(jsonPath("$.data.knowledges[0].teamId").value(teamId))
    }

    @Test
    @Order(5)
    fun testUpdateKnowledgeSuccess() {
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
                "teamId" to teamId,
                "projectId" to projectId,
                "labels" to listOf("updated", "test2"),
            )

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
            .andExpect(jsonPath("$.data.knowledge.teamId").value(teamId))
    }

    @Test
    @Order(6)
    fun testDeleteKnowledgeSuccess() {
        mockMvc
            .perform(
                delete("/knowledges/$knowledgeId").header("Authorization", "Bearer $creatorToken")
            )
            .andExpect(status().isOk)
    }
}
