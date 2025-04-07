package org.rucca.cheese.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.core.Action
import org.rucca.cheese.auth.core.PermissionEvaluator
import org.rucca.cheese.auth.core.ResourceType
import org.rucca.cheese.common.error.NotFoundError
import org.rucca.cheese.model.*
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.notification.models.toDTO
import org.rucca.cheese.notification.models.toEnum
import org.rucca.cheese.notification.services.NotificationQueryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation::class)
class NotificationTest {

    @Autowired private lateinit var webTestClient: WebTestClient

    @MockkBean private lateinit var notificationQueryService: NotificationQueryService

    @MockkBean private lateinit var jwtService: JwtService

    @MockkBean private lateinit var permissionEvaluator: PermissionEvaluator

    private val currentUserId = 1L
    private val baseUri = "/notifications"

    @BeforeEach
    fun setUp() {
        every { jwtService.getCurrentUserId() } returns currentUserId

        every {
            permissionEvaluator.evaluate<Action, ResourceType>(any(), any(), any(), any())
        } returns true
    }

    private fun createSampleNotificationDTO(
        id: Long,
        type: NotificationType,
        read: Boolean = false,
        entitiesMap: Map<String, ResolvedEntityInfoDTO> =
            mapOf("actor" to ResolvedEntityInfoDTO(id = "2", type = "user", name = "Test Actor")),
        contextMap: Map<String, Any> = mapOf("sample" to "context"),
    ): NotificationDTO {
        return NotificationDTO(
            id = id,
            type = type.toDTO(),
            read = read,
            createdAt = Instant.now().toEpochMilli(),
            entities = entitiesMap,
            contextMetadata = contextMap,
        )
    }

    @Test
    fun `listNotifications should return notifications page on success`() {
        val typeEnum = NotificationType.MENTION
        val notification1 = createSampleNotificationDTO(101L, typeEnum) // Example isActive
        val notification2 = createSampleNotificationDTO(100L, NotificationType.REPLY, read = true)
        val notifications = listOf(notification1, notification2)
        val nextCursor = "nextCursor123"
        val pageStart = "currentCursor"
        val requestedPageSize = 10L
        val servicePageSize = requestedPageSize.toInt() // Service expects Int

        // This should match the structure returned by your service/pagination util
        val pageDto =
            EncodedCursorPageDTO(
                pageStart = pageStart,
                nextStart = nextCursor,
                pageSize = notifications.size,
                hasMore = true,
            )

        // Mock service call - ensure parameters match API call
        coEvery {
            notificationQueryService.getNotificationsForCurrentUser(
                userId = currentUserId,
                cursorEncoded = pageStart, // Pass the 'pageStart' param here
                limit = servicePageSize, // Pass the Int 'pageSize' here
                type = null,
                read = null,
            )
        } returns Pair(notifications, pageDto)

        webTestClient
            .get()
            // Use queryParam for request parameters
            .uri { builder ->
                builder
                    .path(baseUri)
                    .queryParam("pageStart", pageStart)
                    .queryParam("pageSize", requestedPageSize) // Use Long for request
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody(ListNotifications200ResponseDTO::class.java) // Expect specific response DTO
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.message).isEqualTo("Success")
                val data = response.data
                assertThat(data).isNotNull
                assertThat(data.page?.nextStart).isEqualTo(nextCursor)
                assertThat(data.page?.hasMore).isTrue()
                assertThat(data.page?.pageSize)
                    .isEqualTo(notifications.size) // Check returned page size
                assertThat(data.notifications).hasSize(2)
                assertThat(data.notifications[0].id).isEqualTo(101L)
                assertThat(data.notifications[1].id).isEqualTo(100L)
            }

        // Verify service call with correct parameters
        coVerify {
            notificationQueryService.getNotificationsForCurrentUser(
                currentUserId,
                pageStart,
                servicePageSize,
                null,
                null,
            )
        }
    }

    @Test
    fun `listNotifications should handle type and read filters`() {
        val typeFilterDTO = NotificationTypeDTO.MENTION
        val readFilter = false
        val expectedTypeEnum = typeFilterDTO.toEnum()
        val requestedPageSize = 20L
        val servicePageSize = requestedPageSize.toInt()
        val pageStart = "currentCursor"

        val notifications =
            listOf(createSampleNotificationDTO(101L, expectedTypeEnum, read = false))
        val pageDto =
            EncodedCursorPageDTO(
                pageStart = pageStart,
                nextStart = null,
                pageSize = 1,
                hasMore = false,
            )

        coEvery {
            notificationQueryService.getNotificationsForCurrentUser(
                currentUserId,
                null,
                servicePageSize,
                expectedTypeEnum,
                readFilter,
            )
        } returns Pair(notifications, pageDto)

        webTestClient
            .get()
            .uri { builder ->
                builder
                    .path(baseUri)
                    .queryParam("pageSize", requestedPageSize)
                    .queryParam("type", typeFilterDTO.value) // Use DTO value for query param
                    .queryParam("read", readFilter.toString())
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ListNotifications200ResponseDTO::class.java)
            .value { response ->
                assertThat(response.data.notifications).hasSize(1)
                assertThat(response.data.notifications[0].type).isEqualTo(typeFilterDTO)
                assertThat(response.data.notifications[0].read).isEqualTo(readFilter)
                assertThat(response.data.page?.hasMore).isFalse()
            }

        coVerify {
            notificationQueryService.getNotificationsForCurrentUser(
                currentUserId,
                null,
                servicePageSize,
                expectedTypeEnum,
                readFilter,
            )
        }
    }

    @Test
    fun `getNotificationById should return notification on success`() {
        val notificationId = 101L
        val typeEnum = NotificationType.MENTION
        val notificationDto = createSampleNotificationDTO(notificationId, typeEnum)

        coEvery {
            notificationQueryService.getNotificationByIdForCurrentUser(
                currentUserId,
                notificationId,
            )
        } returns notificationDto

        webTestClient
            .get()
            .uri("$baseUri/$notificationId") // Path variable
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody(GetNotificationById200ResponseDTO::class.java)
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.data.notification.id).isEqualTo(notificationId)
                assertThat(response.data.notification.type.value)
                    .isEqualTo(typeEnum.name) // Compare DTO value with enum name
            }

        coVerify {
            notificationQueryService.getNotificationByIdForCurrentUser(
                currentUserId,
                notificationId,
            )
        }
    }

    @Test
    fun `getNotificationById should return 404 if service throws NotFoundError`() {
        val notificationId = 999L

        coEvery {
            notificationQueryService.getNotificationByIdForCurrentUser(
                currentUserId,
                notificationId,
            )
        } throws NotFoundError("notification", notificationId) // Use your actual exception

        webTestClient
            .get()
            .uri("$baseUri/$notificationId")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound // Expect 404 based on default Spring Boot exception handling or custom
        // handler

        coVerify {
            notificationQueryService.getNotificationByIdForCurrentUser(
                currentUserId,
                notificationId,
            )
        }
    }

    @Test
    fun `getUnreadNotificationsCount should return count on success`() {
        val count = 5L

        // Mock service call
        coEvery {
            notificationQueryService.getUnreadNotificationCountForCurrentUser(currentUserId)
        } returns count

        webTestClient
            .get()
            .uri("$baseUri/unread-count") // Path from API definition
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GetUnreadNotificationsCount200ResponseDTO::class.java)
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.data.count).isEqualTo(count)
            }

        coVerify {
            notificationQueryService.getUnreadNotificationCountForCurrentUser(currentUserId)
        }
    }

    @Test
    fun `updateNotificationStatus should return updated notification on success`() {
        val notificationId = 101L
        val desiredReadStatus = true
        val requestDto = UpdateNotificationStatusRequestDTO(read = desiredReadStatus)
        // Prepare the expected DTO returned by the service AFTER the update
        val updatedDto =
            createSampleNotificationDTO(notificationId, NotificationType.MENTION, read = true)

        coEvery {
            notificationQueryService.setReadStatus(currentUserId, notificationId, desiredReadStatus)
        } returns updatedDto

        webTestClient
            .patch() // Method from API definition
            .uri("$baseUri/$notificationId") // Path from API definition
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestDto))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(
                GetNotificationById200ResponseDTO::class.java
            ) // Expect specific response type
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.data.notification.id).isEqualTo(notificationId)
                assertThat(response.data.notification.read).isTrue() // Check the updated status
            }

        coVerify {
            notificationQueryService.setReadStatus(currentUserId, notificationId, desiredReadStatus)
        }
    }

    @Test
    fun `updateNotificationStatus should return 404 if service throws NotFoundError`() {
        val notificationId = 999L
        val desiredReadStatus = true
        val requestDto = UpdateNotificationStatusRequestDTO(read = desiredReadStatus)

        coEvery {
            notificationQueryService.setReadStatus(currentUserId, notificationId, desiredReadStatus)
        } throws NotFoundError("notification", notificationId)

        webTestClient
            .patch() // Method from API definition
            .uri("$baseUri/$notificationId") // Path from API definition
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestDto))
            .exchange()
            .expectStatus()
            .isNotFound

        coVerify {
            notificationQueryService.setReadStatus(currentUserId, notificationId, desiredReadStatus)
        }
    }

    @Test
    fun `bulkUpdateNotifications should return updated IDs on success`() {
        val update1 = BulkUpdateNotificationsRequestUpdatesInnerDTO(id = 101L, read = true)
        val update2 = BulkUpdateNotificationsRequestUpdatesInnerDTO(id = 102L, read = false)
        val requestDto = BulkUpdateNotificationsRequestDTO(updates = listOf(update1, update2))
        // This is the format expected by the service method based on previous refactor
        val serviceInput = listOf(101L to true, 102L to false)
        val updatedIds = listOf(101L) // Assume service returns only successfully updated ones

        coEvery { notificationQueryService.bulkSetReadStatus(currentUserId, serviceInput) } returns
            updatedIds

        webTestClient
            .patch() // Method from API definition
            .uri(baseUri) // Path from API definition
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestDto))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(BulkUpdateNotifications200ResponseDTO::class.java)
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.data.updatedIds).isEqualTo(updatedIds)
            }

        // Verify service call with the correctly transformed input
        coVerify { notificationQueryService.bulkSetReadStatus(currentUserId, serviceInput) }
    }

    @Test
    fun `setCollectiveNotificationStatus should return updated count on success`() {
        val requestDto = SetCollectiveNotificationStatusRequestDTO(read = true) // Must be true
        val count = 5 // Service returns Int

        coEvery { notificationQueryService.markAllAsReadForCurrentUser(currentUserId) } returns
            count

        webTestClient
            .put() // Method from API definition
            .uri("$baseUri/status") // Path from API definition
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestDto))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(SetCollectiveNotificationStatus200ResponseDTO::class.java)
            .value { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.data.count).isEqualTo(count)
            }

        coVerify { notificationQueryService.markAllAsReadForCurrentUser(currentUserId) }
    }

    @Test
    fun `setCollectiveNotificationStatus should return 400 if read is false`() {
        val requestDto =
            SetCollectiveNotificationStatusRequestDTO(
                read = false
            ) // Invalid according to controller logic

        webTestClient
            .put() // Method from API definition
            .uri("$baseUri/status") // Path from API definition
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestDto))
            .exchange()
            .expectStatus()
            .isBadRequest // Expect 400 based on controller's check

        coVerify(exactly = 0) { notificationQueryService.markAllAsReadForCurrentUser(any()) }
    }

    @Test
    fun `deleteNotification should return 204 on success`() {
        val notificationId = 101L

        coEvery {
            notificationQueryService.deleteNotificationForCurrentUser(currentUserId, notificationId)
        } just runs

        webTestClient
            .delete() // Method from API definition
            .uri("$baseUri/$notificationId") // Path from API definition
            .exchange()
            .expectStatus()
            .isNoContent // Expect 204

        coVerify {
            notificationQueryService.deleteNotificationForCurrentUser(currentUserId, notificationId)
        }
    }

    @Test
    fun `deleteNotification should return 404 if service throws NotFoundError`() {
        val notificationId = 999L

        coEvery {
            notificationQueryService.deleteNotificationForCurrentUser(currentUserId, notificationId)
        } throws NotFoundError("notification", notificationId)

        webTestClient
            .delete() // Method from API definition
            .uri("$baseUri/$notificationId") // Path from API definition
            .exchange()
            .expectStatus()
            .isNotFound // Expect 404

        coVerify {
            notificationQueryService.deleteNotificationForCurrentUser(currentUserId, notificationId)
        }
    }
}
