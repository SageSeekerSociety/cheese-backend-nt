package org.rucca.cheese.notification

import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.UseNewAuth
import org.rucca.cheese.auth.spring.Auth
import org.rucca.cheese.auth.spring.ResourceId
import org.rucca.cheese.common.error.BadRequestError
import org.rucca.cheese.model.*
import org.rucca.cheese.notification.models.toEnum
import org.rucca.cheese.notification.services.NotificationQueryService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseNewAuth
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
    private val jwtService: JwtService,
) : NotificationsApi {
    private val log = LoggerFactory.getLogger(javaClass)

    @Auth("notification:update:notification")
    override suspend fun bulkUpdateNotifications(
        @Valid bulkUpdateNotificationsRequestDTO: BulkUpdateNotificationsRequestDTO
    ): ResponseEntity<BulkUpdateNotifications200ResponseDTO> {
        val updates =
            bulkUpdateNotificationsRequestDTO.updates.map { update -> Pair(update.id, update.read) }

        val userId = jwtService.getCurrentUserId()

        val updatedIds =
            withContext(Dispatchers.IO) {
                notificationQueryService.bulkSetReadStatus(userId, updates)
            }

        val responseData = BulkUpdateNotifications200ResponseDataDTO(updatedIds = updatedIds)
        return ResponseEntity.ok(
            BulkUpdateNotifications200ResponseDTO(
                code = 200,
                message = "Success",
                data = responseData,
            )
        )
    }

    @Auth("notification:delete:notification")
    override suspend fun deleteNotification(
        @ResourceId notificationId: Long
    ): ResponseEntity<Unit> {
        val userId = jwtService.getCurrentUserId()
        withContext(Dispatchers.IO) {
            notificationQueryService.deleteNotificationForCurrentUser(userId, notificationId)
        }
        return ResponseEntity.noContent().build() // HTTP 204
    }

    @Auth("notification:view:notification")
    override suspend fun getNotificationById(
        @ResourceId notificationId: Long
    ): ResponseEntity<GetNotificationById200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()

        val notificationDto =
            notificationQueryService.getNotificationByIdForCurrentUser(userId, notificationId)

        return ResponseEntity.ok(
            GetNotificationById200ResponseDTO(
                code = 200,
                message = "Success",
                data = GetNotificationById200ResponseDataDTO(notificationDto),
            )
        )
    }

    @Auth("notification:list:notification")
    override suspend fun getUnreadNotificationsCount():
        ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val count = notificationQueryService.getUnreadNotificationCountForCurrentUser(userId)
        val responseData =
            GetUnreadNotificationsCount200ResponseDataDTO(count = count) // DTO expects Long
        return ResponseEntity.ok(
            GetUnreadNotificationsCount200ResponseDTO(
                code = 200,
                message = "Success",
                data = responseData,
            )
        )
    }

    @Auth("notification:list:notification")
    override suspend fun listNotifications(
        pageStart: String?,
        pageSize: Int,
        type: NotificationTypeDTO?,
        read: Boolean?,
    ): ResponseEntity<ListNotifications200ResponseDTO> {
        val notificationType = type?.toEnum()

        val userId = jwtService.getCurrentUserId()

        val (notifications, pageDto) =
            notificationQueryService.getNotificationsForCurrentUser(
                userId,
                pageStart,
                pageSize.toInt().coerceIn(1, 100),
                notificationType,
                read,
            )

        val responseData =
            ListNotifications200ResponseDataDTO(notifications = notifications, page = pageDto)

        return ResponseEntity.ok(
            ListNotifications200ResponseDTO(code = 200, message = "Success", data = responseData)
        )
    }

    @Auth("notification:update:notification")
    override suspend fun setCollectiveNotificationStatus(
        @Valid setCollectiveNotificationStatusRequestDTO: SetCollectiveNotificationStatusRequestDTO
    ): ResponseEntity<SetCollectiveNotificationStatus200ResponseDTO> {
        if (!setCollectiveNotificationStatusRequestDTO.read) {
            log.warn("Attempted to use setCollectiveNotificationStatus with read=false")
            throw BadRequestError(
                "This operation only supports marking all notifications as read (read must be true)."
            )
        }

        val userId = jwtService.getCurrentUserId()

        val count =
            withContext(Dispatchers.IO) {
                notificationQueryService.markAllAsReadForCurrentUser(userId)
            }

        val responseData = SetCollectiveNotificationStatus200ResponseDataDTO(count = count)
        return ResponseEntity.ok(
            SetCollectiveNotificationStatus200ResponseDTO(
                code = 200,
                message = "Success",
                data = responseData,
            )
        )
    }

    @Auth("notification:update:notification")
    override suspend fun updateNotificationStatus(
        @ResourceId notificationId: Long,
        @Valid updateNotificationStatusRequestDTO: UpdateNotificationStatusRequestDTO,
    ): ResponseEntity<GetNotificationById200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val desiredReadStatus = updateNotificationStatusRequestDTO.read
        val updatedNotificationDto =
            notificationQueryService.setReadStatus(userId, notificationId, desiredReadStatus)

        return ResponseEntity.ok(
            GetNotificationById200ResponseDTO(
                code = 200,
                message = "Success",
                data = GetNotificationById200ResponseDataDTO(updatedNotificationDto),
            )
        )
    }
}
