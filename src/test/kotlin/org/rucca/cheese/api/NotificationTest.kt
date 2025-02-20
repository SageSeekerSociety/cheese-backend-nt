package org.rucca.cheese.api

import org.hamcrest.Matchers
import org.junit.Assume
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
                        "projectId": null,
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

        assert(notificationId > 0) { "notificationId should be valid but got $notificationId" }
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
            .andExpect(jsonPath("$.data.notifications", Matchers.hasSize<Collection<*>>(Matchers.greaterThan(0))))
    }

    @Test
    @Order(3)
    fun markNotificationsAsRead() {
        Assume.assumeTrue(notificationId > 0)

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

        // 验证已读
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notifications")
                .header("Authorization", "Bearer $token"))
            .andExpect(jsonPath("$.data.notifications[0].read").value(true))
    }

    @Test
    @Order(4)
    fun getUnreadNotificationsCount() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/notifications/unread/count")
                .header("Authorization", "Bearer $token")
                .queryParam("receiverId", user.userId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.count").isNumber)
    }

    @Test
    @Order(5)
    fun deleteNotification() {
        Assume.assumeTrue(notificationId > 0)

        mockMvc
            .perform(MockMvcRequestBuilders.delete("/notifications/$notificationId")
                .header("Authorization", "Bearer $token"))
            .andExpect(MockMvcResultMatchers.status().isOk)

        mockMvc
            .perform(MockMvcRequestBuilders.get("/notifications/$notificationId")
                .header("Authorization", "Bearer $token"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

}
