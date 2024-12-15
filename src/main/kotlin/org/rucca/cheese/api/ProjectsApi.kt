package org.rucca.cheese.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.rucca.cheese.model.GetProject200ResponseDTO
import org.rucca.cheese.model.GetProjects200ResponseDTO
import org.rucca.cheese.model.PostProjectRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import javax.validation.Valid

/**
 * @author Qhbee
 * @version 1.0 2024/12/15 下午3:33
 */
interface ProjectsApi {

    @Operation(
        tags =
        [
            "default",
        ],
        summary = "Create Project",
        operationId = "postProject",
        description = """""",
        responses =
        [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                [Content(schema = Schema(implementation = GetProject200ResponseDTO::class))]
            )
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/projects"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    fun postProject(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        postProjectRequestDTO: PostProjectRequestDTO
    ): ResponseEntity<GetProject200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags =
        [
            "default",
        ],
        summary = "Enumerate Projects",
        operationId = "getProjects",
        description = """""",
        responses =
        [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                [Content(schema = Schema(implementation = GetProjects200ResponseDTO::class))]
            )
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/projects"],
        produces = ["application/json"]
    )
    fun getProjects(
        @Parameter(
            description = "List Projects",
            schema = Schema(defaultValue = "false")
        )

        @Parameter(description = "Page Size")
        @Valid
        @RequestParam(value = "page_size", required = false)
        pageSize: kotlin.Int?,
        @Parameter(description = "ID of First Element")
        @Valid
        @RequestParam(value = "page_start", required = false)
        pageStart: kotlin.Long?,
        @Parameter(
            description = "\"createdAt\" or \"updatedAt\"",
            schema = Schema(defaultValue = "createdAt")
        )
        @Valid
        @RequestParam(value = "sort_by", required = false, defaultValue = "createdAt")
        sortBy: kotlin.String,
        @Parameter(description = "\"asc\" or \"desc\"", schema = Schema(defaultValue = "desc"))
        @Valid
        @RequestParam(value = "sort_order", required = false, defaultValue = "desc")
        sortOrder: kotlin.String
    ): ResponseEntity<GetProjects200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}