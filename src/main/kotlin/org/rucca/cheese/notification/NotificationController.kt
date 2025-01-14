package org.rucca.cheese.notification

import io.swagger.v3.oas.annotations.Parameter
import javax.annotation.PostConstruct
import org.rucca.cheese.api.NotificationsApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.AuthorizationService
import org.rucca.cheese.auth.AuthorizedAction
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.common.persistent.IdGetter
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.model.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotNull

@RestController
class NotificationController(
    private val notificationService: NotificationService,
    private val authorizationService: AuthorizationService,
    private val authenticationService: AuthenticationService,
) : NotificationsApi {

    @PostConstruct
    fun initialize() {
        authorizationService.ownerIds.register(
            "notifications",
            notificationService::getNotificationOwner
        )
        authorizationService.customAuthLogics.register("is-notification-admin") {
            userId: IdType,
            _: AuthorizedAction,
            _: String,
            resourceId: IdType?,
            _: Map<String, Any?>?,
            _: IdGetter?,
            _: Any?,
            ->
            notificationService.isNotificationAdmin(
                resourceId ?: throw IllegalArgumentException("resourceId is null"),
                userId
            )
        }
    }

    fun createNotification(notification: Notification) {
        notificationService.createNotification(notification)
    }

    fun deleteNotification(notificationId: Long) {
        notificationService.deleteNotification(notificationId)
    }

    @Guard("list-notifications", "notifications")
    override fun listNotifications(
        @NotNull
        @Parameter(description = "Page Start Index", required = true)
        @Valid
        @RequestParam(value = "page_start", required = true)
        pageStart: kotlin.Long,
        @NotNull
        @Parameter(description = "Page Size", required = true)
        @Valid
        @RequestParam(value = "page_size", required = true)
        pageSize: kotlin.Int,
        @Parameter(description = "Notification Type")
        @Valid
        @RequestParam(value = "type", required = false)
        type: kotlin.Any?,
        @Parameter(description = "Whether to filter read notifications")
        @Valid
        @RequestParam(value = "read", required = false)
        read: kotlin.Boolean?,
): ResponseEntity<ListNotifications200ResponseDTO> {
        val notifications =
            notificationService.listNotifications(
                when (type) {
                    null -> null
                    else -> NotificationType.valueOf(type.toString())
                },
                read,
                pageStart,
                pageSize
            )
        return ResponseEntity.ok(
            ListNotifications200ResponseDTO(
                0,
                "success",
                ListNotifications200ResponseDataDTO(notifications.first, notifications.second)
            )
        )
    }

    @Guard("mark-as-read", "notifications")
    override fun markNotificationsAsRead(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        markNotificationsAsReadRequestDTO: MarkNotificationsAsReadRequestDTO
    ): ResponseEntity<kotlin.Any> {
        notificationService.markAsRead(markNotificationsAsReadRequestDTO.notificationIds)
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Guard("get-unread-count", "notifications")
    override fun getUnreadNotificationsCount(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        getUnreadNotificationsCountRequestDTO: GetUnreadNotificationsCountRequestDTO,
    ): ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        return ResponseEntity.ok(
            GetUnreadNotificationsCount200ResponseDTO(
                0,
                "success",
                GetUnreadNotificationsCount200ResponseDataDTO(
                    notificationService.getUnreadCount(
                        getUnreadNotificationsCountRequestDTO.receiverId
                    )
                )
            )
        )
    }
}
