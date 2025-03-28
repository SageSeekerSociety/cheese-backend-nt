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
import org.rucca.cheese.model.CommonResponseDTO
import org.rucca.cheese.model.GetMyTeams200ResponseDTO
import org.rucca.cheese.model.GetTeam200ResponseDTO
import org.rucca.cheese.model.GetTeamMembers200ResponseDTO
import org.rucca.cheese.model.GetTeams200ResponseDTO
import org.rucca.cheese.model.PatchTeamMemberRequestDTO
import org.rucca.cheese.model.PatchTeamRequestDTO
import org.rucca.cheese.model.PostTeamMemberRequestDTO
import org.rucca.cheese.model.PostTeamRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface TeamsApi {

    @Operation(
        tags = ["default"],
        summary = "Delete Team",
        operationId = "deleteTeam",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = [Content(schema = Schema(implementation = CommonResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/teams/{teamId}"],
        produces = ["application/json"],
    )
    fun deleteTeam(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long
    ): ResponseEntity<CommonResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Kick Out Team Member",
        operationId = "deleteTeamMember",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeam200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/teams/{teamId}/members/{userId}"],
        produces = ["application/json"],
    )
    fun deleteTeamMember(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "Member User ID", required = true)
        @PathVariable("userId")
        userId: kotlin.Long,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Query My Teams",
        operationId = "getMyTeams",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetMyTeams200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/my-teams"],
        produces = ["application/json"],
    )
    fun getMyTeams(): ResponseEntity<GetMyTeams200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Query Team",
        operationId = "getTeam",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeam200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/{teamId}"],
        produces = ["application/json"],
    )
    fun getTeam(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Enumerate Team Members",
        operationId = "getTeamMembers",
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
                                    Schema(implementation = GetTeamMembers200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/{teamId}/members"],
        produces = ["application/json"],
    )
    fun getTeamMembers(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(
            description = "Whether to query real name verification status of members",
            schema = Schema(defaultValue = "false"),
        )
        @Valid
        @RequestParam(value = "queryRealNameStatus", required = false, defaultValue = "false")
        queryRealNameStatus: kotlin.Boolean,
    ): ResponseEntity<GetTeamMembers200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Get Teams",
        operationId = "getTeams",
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
        value = ["/teams"],
        produces = ["application/json"],
    )
    fun getTeams(
        @Parameter(description = "ID or Search Term", schema = Schema(defaultValue = ""))
        @Valid
        @RequestParam(value = "query", required = false, defaultValue = "")
        query: kotlin.String,
        @Parameter(description = "")
        @Valid
        @RequestParam(value = "page_start", required = false)
        pageStart: kotlin.Long?,
        @Parameter(description = "", schema = Schema(defaultValue = "20"))
        @Valid
        @RequestParam(value = "page_size", required = false, defaultValue = "20")
        pageSize: kotlin.Int,
    ): ResponseEntity<GetTeams200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Update Team",
        operationId = "patchTeam",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeam200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/teams/{teamId}"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun patchTeam(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        patchTeamRequestDTO: PatchTeamRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Update Team Membership Info",
        operationId = "patchTeamMember",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeam200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/teams/{teamId}/members/{userId}"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun patchTeamMember(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "Member User ID", required = true)
        @PathVariable("userId")
        userId: kotlin.Long,
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        patchTeamMemberRequestDTO: PatchTeamMemberRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Create Team",
        operationId = "postTeam",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeam200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun postTeam(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        postTeamRequestDTO: PostTeamRequestDTO
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Add Team Member",
        operationId = "postTeamMember",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [Content(schema = Schema(implementation = GetTeam200ResponseDTO::class))],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams/{teamId}/members"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun postTeamMember(
        @Parameter(description = "Team ID", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        postTeamMemberRequestDTO: PostTeamMemberRequestDTO,
    ): ResponseEntity<GetTeam200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
