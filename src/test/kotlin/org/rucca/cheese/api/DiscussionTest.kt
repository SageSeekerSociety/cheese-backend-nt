package org.rucca.cheese.api

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.floor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.discussion.ReactionType
import org.rucca.cheese.discussion.ReactionTypeRepository
import org.rucca.cheese.model.CreateProjectRequestDTO
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class DiscussionTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
    private val objectMapper: ObjectMapper,
    private val reactionTypeRepository: ReactionTypeRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    // 存储创建的团队ID
    private var teamId: IdType = -1
    // 存储创建的项目ID
    private var projectId: IdType = -1
    // 存储创建的讨论ID
    private var createdDiscussionId: Long = 0
    // 存储使用的反应类型ID
    private var reactionTypeId: IdType = -1

    /** 创建测试团队 */
    private fun createTeam(
        creatorToken: String,
        teamName: String,
        teamIntro: String,
        teamDescription: String,
        teamAvatarId: IdType,
    ): IdType {
        val request =
            MockMvcRequestBuilders.post("/teams")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                  "name": "$teamName",
                  "intro": "$teamIntro",
                  "description": "$teamDescription",
                  "avatarId": $teamAvatarId
                }
            """
                )
        val teamId =
            JSONObject(mockMvc.perform(request).andReturn().response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("team")
                .getLong("id")
        logger.info("Created team with ID: {}", teamId)
        return teamId
    }

    /** 创建测试项目 */
    private fun createProject(creatorToken: String, teamId: IdType, creatorId: IdType): IdType {
        val request =
            CreateProjectRequestDTO(
                name = "Test Project",
                description = "Test Description",
                colorCode = "#FFFFFF",
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + 86400000,
                teamId = teamId,
                leaderId = creatorId,
                content = "Test Content",
                parentId = null,
                externalTaskId = null,
                githubRepo = null,
            )

        val result =
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/projects")
                        .header("Authorization", "Bearer $creatorToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk)
                .andReturn()

        val projectId =
            JSONObject(result.response.contentAsString)
                .getJSONObject("data")
                .getJSONObject("project")
                .getLong("id")

        logger.info("Created project with ID: {}", projectId)
        return projectId
    }

    @BeforeAll
    fun prepare() {
        // 创建用户
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)

        // 创建团队
        teamId =
            createTeam(
                creatorToken,
                teamName = "Test Team (${floor(Math.random() * 10000000000).toLong()})",
                teamIntro = "This is a test team.",
                teamDescription = "A lengthy text. ".repeat(1000),
                teamAvatarId = userCreatorService.testAvatarId(),
            )

        // 创建项目
        projectId = createProject(creatorToken, teamId, creator.userId)

        // 检查是否存在反应类型，如果没有则创建一个
        val reactionCode = "thumbs_up"
        if (!reactionTypeRepository.existsByCode(reactionCode)) {
            val reactionType =
                ReactionType(
                    code = reactionCode,
                    name = "👍 Thumbs Up",
                    description = "A thumbs up reaction",
                    displayOrder = 1,
                    isActive = true,
                )
            reactionTypeRepository.save(reactionType)
            logger.info("Created reaction type with code: {}", reactionCode)
        }

        // 获取反应类型ID
        val reactionType = reactionTypeRepository.findByCode(reactionCode)
        reactionTypeId =
            reactionType?.id
                ?: throw IllegalStateException("ReactionType with code '$reactionCode' not found")
        logger.info("Using reaction type ID: {}", reactionTypeId)
    }

    @Test
    @Order(1)
    fun testGetDiscussions() {
        val request =
            MockMvcRequestBuilders.get("/discussions")
                .header("Authorization", "Bearer $creatorToken")
                .param("modelType", "PROJECT")
                .param("modelId", projectId.toString())
                .param("pageStart", "0")
                .param("pageSize", "10")
                .param("sortBy", "createdAt")
                .param("sortOrder", "desc")

        mockMvc.perform(request).andExpect(status().isOk)
    }

    @Test
    @Order(2)
    fun testCreateDiscussion() {
        // 创建内容对象
        val contentObj = mapOf("text" to "Test discussion content")

        // 创建请求数据
        val requestData =
            mapOf(
                "content" to Json.encodeToString(contentObj),
                "parentId" to null,
                "mentionedUserIds" to emptyList<Long>(),
                "modelType" to "PROJECT",
                "modelId" to projectId,
            )

        val request =
            MockMvcRequestBuilders.post("/discussions")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestData))

        val result = mockMvc.perform(request).andExpect(status().isOk).andReturn()

        // 解析响应获取创建的讨论ID
        val responseBody = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseBody, Map::class.java)
        val data = responseMap["data"] as Map<*, *>
        val discussion = data["discussion"] as Map<*, *>
        createdDiscussionId = (discussion["id"] as Number).toLong()
        logger.info("Created discussion with ID: {}", createdDiscussionId)
    }

    @Test
    @Order(3)
    fun `test POST discussion reaction endpoint`() {
        // 确保已创建讨论
        Assertions.assertTrue(
            createdDiscussionId > 0,
            "Discussion should be created before testing reactions",
        )

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                        "/discussions/$createdDiscussionId/reactions/$reactionTypeId"
                    )
                    .header("Authorization", "Bearer $creatorToken")
                    .contentType("application/json")
            )
            .andExpect(status().isOk)
    }
}
