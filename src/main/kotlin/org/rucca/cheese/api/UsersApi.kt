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
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.rucca.cheese.model.GetUserIdentity200ResponseDTO
import org.rucca.cheese.model.GetUserIdentityAccessLogs200ResponseDTO
import org.rucca.cheese.model.PutUserIdentity200ResponseDTO
import org.rucca.cheese.model.PutUserIdentityRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface UsersApi {

    @Operation(
        tags = ["Users"],
        summary = "Get User Real Name Identity Info",
        operationId = "getUserIdentity",
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
                                    Schema(implementation = GetUserIdentity200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/users/{userId}/identity"],
        produces = ["application/json"],
    )
    suspend fun getUserIdentity(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "The unique identifier of the user.", required = true)
        @PathVariable("userId")
        userId: kotlin.Long,
        @Parameter(
            description = "Whether to return the precise identity info",
            schema = Schema(defaultValue = "false"),
        )
        @Valid
        @RequestParam(value = "precise", required = false, defaultValue = "false")
        precise: kotlin.Boolean,
    ): ResponseEntity<GetUserIdentity200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["Users"],
        summary = "Get User Real Name Identity Access Logs",
        operationId = "getUserIdentityAccessLogs",
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
                                            GetUserIdentityAccessLogs200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/users/{userId}/identity/access-logs"],
        produces = ["application/json"],
    )
    suspend fun getUserIdentityAccessLogs(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "The unique identifier of the user.", required = true)
        @PathVariable("userId")
        userId: kotlin.Long,
        @Parameter(description = "The ID of the first item in the page.")
        @Valid
        @RequestParam(value = "pageStart", required = false)
        pageStart: kotlin.Long?,
        @Min(1)
        @Max(100)
        @Parameter(
            description = "The number of items per page.",
            schema = Schema(defaultValue = "20"),
        )
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "20")
        pageSize: kotlin.Int,
    ): ResponseEntity<GetUserIdentityAccessLogs200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["Users"],
        summary = "Update User Real Name Identity Info",
        operationId = "patchUserIdentity",
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
                                    Schema(implementation = PutUserIdentity200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/users/{userId}/identity"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun patchUserIdentity(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "The unique identifier of the user.", required = true)
        @PathVariable("userId")
        userId: kotlin.Long,
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        putUserIdentityRequestDTO: PutUserIdentityRequestDTO,
    ): ResponseEntity<PutUserIdentity200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["Users"],
        summary = "Update User Real Name Identity Info",
        operationId = "putUserIdentity",
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
                                    Schema(implementation = PutUserIdentity200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PUT],
        value = ["/users/{userId}/identity"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun putUserIdentity(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "The unique identifier of the user.", required = true)
        @PathVariable("userId")
        userId: kotlin.Long,
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        putUserIdentityRequestDTO: PutUserIdentityRequestDTO,
    ): ResponseEntity<PutUserIdentity200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
