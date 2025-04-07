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
import org.rucca.cheese.model.ApplicationStatusDTO
import org.rucca.cheese.model.CommonResponseDTO
import org.rucca.cheese.model.CreateTeamInvitation201ResponseDTO
import org.rucca.cheese.model.CreateTeamJoinRequest201ResponseDTO
import org.rucca.cheese.model.GetMyTeams200ResponseDTO
import org.rucca.cheese.model.GetTeam200ResponseDTO
import org.rucca.cheese.model.GetTeamMembers200ResponseDTO
import org.rucca.cheese.model.GetTeams200ResponseDTO
import org.rucca.cheese.model.ListTeamInvitations200ResponseDTO
import org.rucca.cheese.model.ListTeamJoinRequests200ResponseDTO
import org.rucca.cheese.model.PatchTeamMemberRequestDTO
import org.rucca.cheese.model.PatchTeamRequestDTO
import org.rucca.cheese.model.PostTeamMemberRequestDTO
import org.rucca.cheese.model.PostTeamRequestDTO
import org.rucca.cheese.model.TeamInvitationCreateDTO
import org.rucca.cheese.model.TeamJoinRequestCreateDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface TeamsApi {

    @Operation(
        tags = ["default"],
        summary = "Approve a join request",
        operationId = "approveTeamJoinRequest",
        description =
            """Approves a pending join request for the team. Requires team admin/owner privileges. Creates the team membership upon success.""",
        responses =
            [
                ApiResponse(
                    responseCode = "204",
                    description = "Request approved successfully and membership created.",
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams/{teamId}/requests/{requestId}/approve"],
    )
    suspend fun approveTeamJoinRequest(
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "The unique identifier of the join request.", required = true)
        @PathVariable("requestId")
        requestId: kotlin.Long,
    ): ResponseEntity<Unit> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["Team Invitations"],
        summary = "Cancel a pending invitation",
        operationId = "cancelTeamInvitation",
        description =
            """Allows the initiator (team admin/owner) to cancel a pending invitation they sent.""",
        responses =
            [ApiResponse(responseCode = "204", description = "Invitation cancelled successfully.")],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/teams/{teamId}/invitations/{invitationId}"],
    )
    suspend fun cancelTeamInvitation(
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "The unique identifier of the invitation.", required = true)
        @PathVariable("invitationId")
        invitationId: kotlin.Long,
    ): ResponseEntity<Unit> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Invite a user to the team",
        operationId = "createTeamInvitation",
        description = """Allows a team admin/owner to invite a user to join the specified team.""",
        responses =
            [
                ApiResponse(
                    responseCode = "201",
                    description =
                        "Invitation sent successfully. Returns the created application details.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = CreateTeamInvitation201ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams/{teamId}/invitations"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun createTeamInvitation(
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(
            description = "Details of the user to invite and optional role/message.",
            required = true,
        )
        @Valid
        @RequestBody
        teamInvitationCreateDTO: TeamInvitationCreateDTO,
    ): ResponseEntity<CreateTeamInvitation201ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Request to join a team",
        operationId = "createTeamJoinRequest",
        description =
            """Allows the authenticated user to submit a request to join the specified team.""",
        responses =
            [
                ApiResponse(
                    responseCode = "201",
                    description =
                        "Join request submitted successfully. Returns the created application details.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = CreateTeamJoinRequest201ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams/{teamId}/requests"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun createTeamJoinRequest(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "Optional message for the join request.")
        @Valid
        @RequestBody(required = false)
        teamJoinRequestCreateDTO: TeamJoinRequestCreateDTO?,
    ): ResponseEntity<CreateTeamJoinRequest201ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/teams/{teamId}"],
        produces = ["application/json"],
    )
    suspend fun deleteTeam(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.DELETE],
        value = ["/teams/{teamId}/members/{userId}"],
        produces = ["application/json"],
    )
    suspend fun deleteTeamMember(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/my-teams"],
        produces = ["application/json"],
    )
    suspend fun getMyTeams(): ResponseEntity<GetMyTeams200ResponseDTO> {
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/{teamId}"],
        produces = ["application/json"],
    )
    suspend fun getTeam(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/{teamId}/members"],
        produces = ["application/json"],
    )
    suspend fun getTeamMembers(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams"],
        produces = ["application/json"],
    )
    suspend fun getTeams(
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
        summary = "List invitations sent by a team",
        operationId = "listTeamInvitations",
        description =
            """Retrieves a list of invitations sent by the specified team. Requires team admin/owner privileges.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "A list of invitations.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = ListTeamInvitations200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/{teamId}/invitations"],
        produces = ["application/json"],
    )
    suspend fun listTeamInvitations(
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(
            description = "Filter invitations by status (e.g., PENDING).",
            schema =
                Schema(
                    allowableValues =
                        ["PENDING", "APPROVED", "REJECTED", "ACCEPTED", "DECLINED", "CANCELED"]
                ),
        )
        @Valid
        @RequestParam(value = "status", required = false)
        status: ApplicationStatusDTO?,
        @Parameter(description = "The ID of the first item in the page.")
        @Valid
        @RequestParam(value = "pageStart", required = false)
        pageStart: kotlin.Long?,
        @Min(1L)
        @Max(100L)
        @Parameter(
            description = "The number of items per page.",
            schema = Schema(defaultValue = "20L"),
        )
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "20")
        pageSize: kotlin.Long,
    ): ResponseEntity<ListTeamInvitations200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "List join requests for a team",
        operationId = "listTeamJoinRequests",
        description =
            """Retrieves a list of join requests directed to the specified team. Requires team admin/owner privileges.""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "A list of join requests.",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = ListTeamJoinRequests200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/teams/{teamId}/requests"],
        produces = ["application/json"],
    )
    suspend fun listTeamJoinRequests(
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(
            description = "Filter requests by status (e.g., PENDING).",
            schema =
                Schema(
                    allowableValues =
                        ["PENDING", "APPROVED", "REJECTED", "ACCEPTED", "DECLINED", "CANCELED"]
                ),
        )
        @Valid
        @RequestParam(value = "status", required = false)
        status: ApplicationStatusDTO?,
        @Parameter(description = "The ID of the first item in the page.")
        @Valid
        @RequestParam(value = "pageStart", required = false)
        pageStart: kotlin.Long?,
        @Min(1L)
        @Max(100L)
        @Parameter(
            description = "The number of items per page.",
            schema = Schema(defaultValue = "20L"),
        )
        @Valid
        @RequestParam(value = "pageSize", required = false, defaultValue = "20")
        pageSize: kotlin.Long,
    ): ResponseEntity<ListTeamJoinRequests200ResponseDTO> {
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/teams/{teamId}"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun patchTeam(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/teams/{teamId}/members/{userId}"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun patchTeamMember(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun postTeam(
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
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams/{teamId}/members"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    suspend fun postTeamMember(
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

    @Operation(
        tags = ["Team Requests"],
        summary = "Reject a join request",
        operationId = "rejectTeamJoinRequest",
        description =
            """Rejects a pending join request for the team. Requires team admin/owner privileges.""",
        responses =
            [ApiResponse(responseCode = "204", description = "Request rejected successfully.")],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/teams/{teamId}/requests/{requestId}/reject"],
    )
    suspend fun rejectTeamJoinRequest(
        @Parameter(description = "The unique identifier of the team.", required = true)
        @PathVariable("teamId")
        teamId: kotlin.Long,
        @Parameter(description = "The unique identifier of the join request.", required = true)
        @PathVariable("requestId")
        requestId: kotlin.Long,
    ): ResponseEntity<Unit> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
