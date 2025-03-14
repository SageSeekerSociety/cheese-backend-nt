/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech)
 * (7.10.0). https://openapi-generator.tech Do not edit the class manually.
 */
package org.rucca.cheese.api

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.enums.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import io.swagger.v3.oas.annotations.security.*
import javax.validation.Valid
import javax.validation.constraints.NotNull
import org.rucca.cheese.model.DeleteNotification200ResponseDTO
import org.rucca.cheese.model.GetTeams200ResponseDTO
import org.rucca.cheese.model.GetUnreadNotificationsCount200ResponseDTO
import org.rucca.cheese.model.MarkNotificationsAsRead200ResponseDTO
import org.rucca.cheese.model.MarkNotificationsAsReadRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface NotificationsApi {

    @Operation(
        tags = ["default"],
        summary = "Delete Notification",
        operationId = "deleteNotification",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(implementation = DeleteNotification200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/notifications/{notificationId}"],
        produces = ["application/json"],
    )
    fun deleteNotification(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable("notificationId")
        notificationId: kotlin.Long
    ): ResponseEntity<DeleteNotification200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Get Unread Notifications Count",
        operationId = "getUnreadNotificationsCount",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
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
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/notifications/unread/count"],
        produces = ["application/json"],
    )
    fun getUnreadNotificationsCount(
        @NotNull
        @Parameter(description = "", required = true)
        @Valid
        @RequestParam(value = "receiverId", required = true)
        receiverId: kotlin.Long
    ): ResponseEntity<GetUnreadNotificationsCount200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "List Notifications",
        operationId = "listNotifications",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeams200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/notifications"],
        produces = ["application/json"],
    )
    fun listNotifications(
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
        @Parameter(
            description = "Notification Type",
            schema =
                Schema(
                    allowableValues =
                        ["mention", "reply", "reaction", "project_invite", "deadline_remind"]
                ),
        )
        @Valid
        @RequestParam(value = "type", required = false)
        type: kotlin.String?,
        @Parameter(description = "Whether to filter read or unread notifications")
        @Valid
        @RequestParam(value = "read", required = false)
        read: kotlin.Boolean?,
    ): ResponseEntity<GetTeams200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Mark Notifications as Read",
        operationId = "markNotificationsAsRead",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation =
                                            MarkNotificationsAsRead200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/notifications/read"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun markNotificationsAsRead(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        markNotificationsAsReadRequestDTO: MarkNotificationsAsReadRequestDTO
    ): ResponseEntity<MarkNotificationsAsRead200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
