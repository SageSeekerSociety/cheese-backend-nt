package org.rucca.cheese.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class DiscussionTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    private val userCreatorService: UserCreatorService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var creator: UserCreatorService.CreateUserResponse
    lateinit var creatorToken: String
    // 存储创建的讨论ID
    private var createdDiscussionId: Long = 0

    @BeforeAll
    fun prepare() {
        creator = userCreatorService.createUser()
        creatorToken = userCreatorService.login(creator.username, creator.password)
    }

    @Test
    @Order(1)
    fun testGetDiscussions() {
        val request =
            MockMvcRequestBuilders.get("/discussions")
                .header("Authorization", "Bearer $creatorToken")
                .param("projectId", "1")
                .param("pageStart", "0")
                .param("pageSize", "10")
                .param("sortBy", "createdAt")
                .param("sortOrder", "desc")

        mockMvc.perform(request).andExpect(status().isOk)
    }

    @Test
    @Order(2)
    fun testCreateDiscussion() {
        val request =
            MockMvcRequestBuilders.post("/discussions")
                .header("Authorization", "Bearer $creatorToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "content": "测试讨论内容",
                    "parentId": null,
                    "mentionedUserIds": [],
                    "projectId": null
                }
            """
                )

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
                MockMvcRequestBuilders.post("/discussions/$createdDiscussionId/reactions")
                    .header("Authorization", "Bearer $creatorToken")
                    .contentType("application/json")
                    .content("""{"emoji": "👍"}""")
            )
            .andExpect(status().isOk)
    }
}
