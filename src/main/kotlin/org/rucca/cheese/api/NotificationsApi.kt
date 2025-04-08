/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech)
 * (7.12.0). https://openapi-generator.tech Do not edit the class manually.
 */
package org.rucca.cheese.api

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.enums.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import io.swagger.v3.oas.annotations.security.*
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import org.rucca.cheese.model.BulkUpdateNotifications200ResponseDTO
import org.rucca.cheese.model.BulkUpdateNotificationsRequestDTO
import org.rucca.cheese.model.GetNotificationById200ResponseDTO
import org.rucca.cheese.model.GetUnreadNotificationsCount200ResponseDTO
import org.rucca.cheese.model.ListNotifications200ResponseDTO
import org.rucca.cheese.model.NotificationTypeDTO
import org.rucca.cheese.model.SetCollectiveNotificationStatus200ResponseDTO
import org.rucca.cheese.model.SetCollectiveNotificationStatusRequestDTO
import org.rucca.cheese.model.UpdateNotificationStatusRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface NotificationsApi {

    @Operation(
        tags = ["default"],
        summary = "Bulk Update Notification Status (e.g., Mark Multiple as Read)",
        operationId = "bulkUpdateNotifications",
        description =
            """Updates the status (e.g., read state) of multiple notifications belonging to the current user in a single request.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description =
                        "Bulk update successful. Returns the IDs that were successfully updated.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation =
                                            BulkUpdateNotifications200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/notifications"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun bulkUpdateNotifications(
        @Parameter(
            description =
                "An array of notification updates. Currently only supports updating the 'read' status.",
            required = true,
        )
        @Valid
        @RequestBody
        bulkUpdateNotificationsRequestDTO: BulkUpdateNotificationsRequestDTO
    ): ResponseEntity<BulkUpdateNotifications200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Delete a Notification",
        operationId = "deleteNotification",
        description =
            """Deletes a specific notification belonging to the currently authenticated user.""",
        responses =
            [ApiResponse(responseCode = "204", description = "Notification successfully deleted.")],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(method = [RequestMethod.DELETE], value = ["/notifications/{notificationId}"])
    suspend fun deleteNotification(
        @Parameter(description = "The unique identifier of the notification.", required = true)
        @PathVariable("notificationId")
        notificationId: kotlin.Long
    ): ResponseEntity<Unit> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Get a Single Notification",
        operationId = "getNotificationById",
        description =
            """Retrieves details of a specific notification belonging to the current user.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "Notification details.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = GetNotificationById200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/notifications/{notificationId}"],
        produces = ["application/json"],
    )
    suspend fun getNotificationById(
        @Parameter(description = "The unique identifier of the notification.", required = true)
        @PathVariable("notificationId")
        notificationId: kotlin.Long
    ): ResponseEntity<GetNotificationById200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Get Unread Notification Count",
        operationId = "getUnreadNotificationsCount",
        description =
            """Retrieves the count of unread notifications for the currently authenticated user. (Pragmatic endpoint).""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "Unread count retrieved.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation =
                                            GetUnreadNotificationsCount200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/notifications/unread-count"],
        produces = ["application/json"],
    )
    suspend fun getUnreadNotificationsCount():
        ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "List Notifications for Current User",
        operationId = "listNotifications",
        description =
            """Retrieves a paginated list of notifications for the currently authenticated user, sorted by creation date descending. Uses cursor-based pagination.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of notifications.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(implementation = ListNotifications200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/notifications"],
        produces = ["application/json"],
    )
    suspend fun listNotifications(
        @Parameter(description = "The cursor of the first item in the page.")
        @Valid
        @RequestParam(value = "pageStart", required = false)
        pageStart: kotlin.String?,
        @Min(1L)
        @Max(100L)
        @Parameter(
            description = "The number of items per page.",
            schema = Schema(defaultValue = "20L"),
        )
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "20")
        pageSize: kotlin.Long,
        @Parameter(
            description = "Filter by notification type.",
            schema =
                Schema(
                    allowableValues =
                        [
                            "MENTION",
                            "REPLY",
                            "REACTION",
                            "PROJECT_INVITE",
                            "DEADLINE_REMIND",
                            "TEAM_JOIN_REQUEST",
                            "TEAM_INVITATION",
                            "TEAM_REQUEST_APPROVED",
                            "TEAM_REQUEST_REJECTED",
                            "TEAM_INVITATION_ACCEPTED",
                            "TEAM_INVITATION_DECLINED",
                            "TEAM_INVITATION_CANCELED",
                            "TEAM_REQUEST_CANCELED",
                        ]
                ),
        )
        @Valid
        @RequestParam(value = "type", required = false)
        type: NotificationTypeDTO?,
        @Parameter(description = "Filter by read status.")
        @Valid
        @RequestParam(value = "read", required = false)
        read: kotlin.Boolean?,
    ): ResponseEntity<ListNotifications200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Set Collective Notification Status (e.g., Mark All as Read)",
        operationId = "setCollectiveNotificationStatus",
        description =
            """Sets a specific status for all applicable notifications of the current user. Currently only supports setting 'read' to true for all unread notifications.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "Collective status updated (all marked as read).",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation =
                                            SetCollectiveNotificationStatus200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PUT],
        value = ["/notifications/status"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun setCollectiveNotificationStatus(
        @Parameter(description = "The status to apply to the collection.", required = true)
        @Valid
        @RequestBody
        setCollectiveNotificationStatusRequestDTO: SetCollectiveNotificationStatusRequestDTO
    ): ResponseEntity<SetCollectiveNotificationStatus200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Update Notification Status (e.g., Mark as Read/Unread)",
        operationId = "updateNotificationStatus",
        description =
            """Updates the status (e.g., read state) of a specific notification belonging to the current user.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description =
                        "Notification updated successfully. Returns the updated notification.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = GetNotificationById200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/notifications/{notificationId}"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun updateNotificationStatus(
        @Parameter(description = "The unique identifier of the notification.", required = true)
        @PathVariable("notificationId")
        notificationId: kotlin.Long,
        @Parameter(
            description =
                "The update to apply. Currently only supports changing the 'read' status.",
            required = true,
        )
        @Valid
        @RequestBody
        updateNotificationStatusRequestDTO: UpdateNotificationStatusRequestDTO,
    ): ResponseEntity<GetNotificationById200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
