package org.rucca.cheese.api

import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.notification.NotificationService
import org.rucca.cheese.notification.NotificationType
import org.rucca.cheese.utils.UserCreatorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class NotificationTest
@Autowired
constructor(private val mockMvc: MockMvc, private val userCreatorService: UserCreatorService) {
    @Autowired private lateinit var notificationService: NotificationService
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var receiver: UserCreatorService.CreateUserResponse
    private lateinit var user: UserCreatorService.CreateUserResponse
    private lateinit var receiverToken: String
    private lateinit var userToken: String
    private var notificationId: IdType = -1

    @BeforeAll
    fun prepare() {
        receiver = userCreatorService.createUser()
        receiverToken = userCreatorService.login(receiver.username, receiver.password)
        user = userCreatorService.createUser()
        userToken = userCreatorService.login(user.username, user.password)
    }

    @BeforeEach
    fun setupNotification() {
        notificationId =
            notificationService.createNotification(
                type = NotificationType.MENTION,
                receiverId = receiver.userId,
                text = "Hello, you were mentioned!",
                projectId = 2001,
            )
        logger.info("Notification ID: $notificationId")
    }

    @Test
    @Order(2)
    fun listNotifications() {
        val request =
            MockMvcRequestBuilders.get("/notifications")
                .header("Authorization", "Bearer $receiverToken")
                .queryParam("page_start", "0")
                .queryParam("page_size", "10")
                .queryParam("type", "mention")

        val response =
            mockMvc
                .perform(request)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(
                    jsonPath(
                        "$.data.notifications",
                        Matchers.hasSize<Collection<*>>(Matchers.greaterThan(0)),
                    )
                )
                .andReturn()
    }

    @Test
    @Order(3)
    fun markNotificationsAsRead() {
        Assumptions.assumeTrue(notificationId > 0)

        val request =
            MockMvcRequestBuilders.post("/notifications/read")
                .header("Authorization", "Bearer $receiverToken")
                .contentType("application/json")
                .content(
                    """
                {
                    "notificationIds": [$notificationId]
                }
                """
                )

        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.notificationIds").isArray)
    }

    @Test
    @Order(4)
    fun getUnreadNotificationsCount() {
        val request =
            MockMvcRequestBuilders.get("/notifications/unread/count")
                .header("Authorization", "Bearer $receiverToken")
                .queryParam("receiverId", receiver.userId.toString())
        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.data.count").isNumber)
            .andReturn()
    }

    @Test
    @Order(61)
    fun testListNotificationsWhenEmpty() {
        val request =
            MockMvcRequestBuilders.get("/notifications")
                .header("Authorization", "Bearer $userToken")
                .queryParam("page_start", "0")
                .queryParam("page_size", "10")

        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.notifications").isArray)
            .andExpect(
                jsonPath(
                    "$.data.notifications",
                    Matchers.hasSize<Collection<*>>(Matchers.equalTo(0)),
                )
            )
    }

    @Test
    @Order(71)
    fun testNonReceiverDeleteNotification() {

        Assumptions.assumeTrue(notificationId > 0)

        val request =
            MockMvcRequestBuilders.delete("/notifications/$notificationId")
                .header("Authorization", "Bearer $userToken")

        mockMvc
            .perform(request)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value(Matchers.containsString("NotFoundError")))
    }

    @Test
    @Order(100)
    fun deleteNotification() {
        Assumptions.assumeTrue(notificationId > 0)

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/notifications/$notificationId")
                    .header("Authorization", "Bearer $receiverToken")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("ok"))
    }
}
