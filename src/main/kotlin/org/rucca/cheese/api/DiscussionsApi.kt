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
import org.rucca.cheese.model.CreateDiscussion200ResponseDTO
import org.rucca.cheese.model.CreateDiscussionRequestDTO
import org.rucca.cheese.model.DiscussableModelTypeDTO
import org.rucca.cheese.model.GetAllReactionTypes200ResponseDTO
import org.rucca.cheese.model.GetDiscussion200ResponseDTO
import org.rucca.cheese.model.ListDiscussions200ResponseDTO
import org.rucca.cheese.model.ListSubDiscussions200ResponseDTO
import org.rucca.cheese.model.PatchDiscussion200ResponseDTO
import org.rucca.cheese.model.PatchDiscussionRequestDTO
import org.rucca.cheese.model.ReactToDiscussion200ResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface DiscussionsApi {

    @Operation(
        tags = ["default"],
        summary = "Create Discussion",
        operationId = "createDiscussion",
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
                                    Schema(implementation = CreateDiscussion200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/discussions"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun createDiscussion(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        createDiscussionRequestDTO: CreateDiscussionRequestDTO
    ): ResponseEntity<CreateDiscussion200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Delete Discussion",
        operationId = "deleteDiscussion",
        description = """""",
        responses = [ApiResponse(responseCode = "204", description = "No Content")],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(method = [RequestMethod.DELETE], value = ["/discussions/{discussionId}"])
    fun deleteDiscussion(
        @Parameter(description = "讨论ID", required = true)
        @PathVariable("discussionId")
        discussionId: kotlin.Long
    ): ResponseEntity<Unit> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["Reaction Types"],
        summary = "Get all reaction types",
        operationId = "getAllReactionTypes",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "Successful operation",
                    content =
                        [
                            Content(
                                schema =
                                    Schema(
                                        implementation = GetAllReactionTypes200ResponseDTO::class
                                    )
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/discussions/reactions"],
        produces = ["application/json"],
    )
    fun getAllReactionTypes(): ResponseEntity<GetAllReactionTypes200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Query Discussion",
        operationId = "getDiscussion",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [
                            Content(
                                schema = Schema(implementation = GetDiscussion200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/discussions/{discussionId}"],
        produces = ["application/json"],
    )
    fun getDiscussion(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "讨论ID", required = true)
        @PathVariable("discussionId")
        discussionId: kotlin.Long,
        @Parameter(description = "起始ID")
        @Valid
        @RequestParam(value = "page_start", required = false)
        pageStart: kotlin.Long?,
        @Parameter(description = "每页数量 (默认20)", schema = Schema(defaultValue = "20"))
        @Valid
        @RequestParam(value = "page_size", required = false, defaultValue = "20")
        pageSize: kotlin.Int,
        @Parameter(
            description = "\"createdAt\" or \"updatedAt\"",
            schema = Schema(defaultValue = "createdAt"),
        )
        @Valid
        @RequestParam(value = "sort_by", required = false, defaultValue = "createdAt")
        sortBy: kotlin.String,
        @Parameter(description = "\"asc\" or \"desc\"", schema = Schema(defaultValue = "desc"))
        @Valid
        @RequestParam(value = "sort_order", required = false, defaultValue = "desc")
        sortOrder: kotlin.String,
    ): ResponseEntity<GetDiscussion200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "List Discussions",
        operationId = "listDiscussions",
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
                                    Schema(implementation = ListDiscussions200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/discussions"],
        produces = ["application/json"],
    )
    fun listDiscussions(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "模型类型", schema = Schema(allowableValues = ["PROJECT"]))
        @Valid
        @RequestParam(value = "modelType", required = false)
        modelType: DiscussableModelTypeDTO?,
        @Parameter(description = "模型ID")
        @Valid
        @RequestParam(value = "modelId", required = false)
        modelId: kotlin.Long?,
        @Parameter(description = "父讨论ID (可选)")
        @Valid
        @RequestParam(value = "parent_id", required = false)
        parentId: kotlin.Long?,
        @Parameter(description = "起始ID")
        @Valid
        @RequestParam(value = "page_start", required = false)
        pageStart: kotlin.Long?,
        @Parameter(description = "每页数量 (默认20)", schema = Schema(defaultValue = "20"))
        @Valid
        @RequestParam(value = "page_size", required = false, defaultValue = "20")
        pageSize: kotlin.Int,
        @Parameter(
            description = "\"createdAt\" or \"updatedAt\"",
            schema = Schema(defaultValue = "createdAt"),
        )
        @Valid
        @RequestParam(value = "sort_by", required = false, defaultValue = "createdAt")
        sortBy: kotlin.String,
        @Parameter(description = "\"asc\" or \"desc\"", schema = Schema(defaultValue = "desc"))
        @Valid
        @RequestParam(value = "sort_order", required = false, defaultValue = "desc")
        sortOrder: kotlin.String,
    ): ResponseEntity<ListDiscussions200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "List Sub-Discussions",
        operationId = "listSubDiscussions",
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
                                    Schema(implementation = ListSubDiscussions200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/discussions/{discussionId}/sub-discussions"],
        produces = ["application/json"],
    )
    fun listSubDiscussions(
        userInfo: org.rucca.cheese.auth.model.AuthUserInfo?,
        @Parameter(description = "讨论ID", required = true)
        @PathVariable("discussionId")
        discussionId: kotlin.Long,
        @Parameter(description = "起始ID")
        @Valid
        @RequestParam(value = "page_start", required = false)
        pageStart: kotlin.Long?,
        @Parameter(description = "每页数量 (默认20)", schema = Schema(defaultValue = "20"))
        @Valid
        @RequestParam(value = "page_size", required = false, defaultValue = "20")
        pageSize: kotlin.Int,
        @Parameter(
            description = "\"createdAt\" or \"updatedAt\"",
            schema = Schema(defaultValue = "createdAt"),
        )
        @Valid
        @RequestParam(value = "sort_by", required = false, defaultValue = "createdAt")
        sortBy: kotlin.String,
        @Parameter(description = "\"asc\" or \"desc\"", schema = Schema(defaultValue = "desc"))
        @Valid
        @RequestParam(value = "sort_order", required = false, defaultValue = "desc")
        sortOrder: kotlin.String,
    ): ResponseEntity<ListSubDiscussions200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Update Discussion",
        operationId = "patchDiscussion",
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
                                    Schema(implementation = PatchDiscussion200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.PATCH],
        value = ["/discussions/{discussionId}"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun patchDiscussion(
        @Parameter(description = "讨论ID", required = true)
        @PathVariable("discussionId")
        discussionId: kotlin.Long,
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        patchDiscussionRequestDTO: PatchDiscussionRequestDTO,
    ): ResponseEntity<PatchDiscussion200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "React to Discussion",
        operationId = "reactToDiscussion",
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
                                    Schema(implementation = ReactToDiscussion200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/discussions/{discussionId}/reactions/{reactionTypeId}"],
        produces = ["application/json"],
    )
    fun reactToDiscussion(
        @Parameter(description = "讨论ID", required = true)
        @PathVariable("discussionId")
        discussionId: kotlin.Long,
        @Parameter(description = "", required = true)
        @PathVariable("reactionTypeId")
        reactionTypeId: kotlin.Long,
    ): ResponseEntity<ReactToDiscussion200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
