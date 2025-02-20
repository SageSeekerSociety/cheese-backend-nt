package org.rucca.cheese.api

import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class NotificationTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var user: UserCreatorService.CreateUserResponse
    private lateinit var token: String
    private var notificationId: IdType = -1

    @BeforeAll
    fun prepare() {
        user = userCreatorService.createUser()
        token = userCreatorService.login(user.username, user.password)
    }

    @Test
    @Order(1)
    fun createNotification() {
        val request =
            MockMvcRequestBuilders.post("/notifications")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                {
                    "type": "mention",
                    "receiverId": ${user.userId},
                    "content": {
                        "text": "Hello, you were mentioned!",
                        "projectId": 2001,
                        "discussionId": null,
                        "knowledgeId": null
                    }
                }
                """
                )

        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.notification.type").value("mention"))
                .andReturn()

        val json = org.json.JSONObject(response.response.contentAsString)
        notificationId = json.getJSONObject("data").getJSONObject("notification").getLong("id")

        logger.info("Created notification ID: $notificationId")
    }

    @Test
    @Order(2)
    fun listNotifications() {
        val request =
            MockMvcRequestBuilders.get("/notifications")
                .header("Authorization", "Bearer $token")
                .queryParam("page_start", "0")
                .queryParam("page_size", "10")

        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.notifications").isArray)
    }

    @Test
    @Order(3)
    fun markNotificationsAsRead() {
        val request =
            MockMvcRequestBuilders.post("/notifications/read")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                {
                    "notificationIds": [$notificationId]
                }
                """
                )

        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @Order(4)
    fun getUnreadNotificationsCount() {
        val request =
            MockMvcRequestBuilders.get("/notifications/unread/count")
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .content(
                    """
                {
                    "receiverId": ${user.userId}
                }
                """
                )

        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.count").isNumber)
    }

    @Test
    @Order(5)
    fun deleteNotification() {
        val request =
            MockMvcRequestBuilders.delete("/notifications/$notificationId")
                .header("Authorization", "Bearer $token")

        logger.info("Deleting notification ID: $notificationId")
        logger.info("Token: $token")
        logger.info("User ID: ${user.userId}")

        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("ok"))
    }
}
