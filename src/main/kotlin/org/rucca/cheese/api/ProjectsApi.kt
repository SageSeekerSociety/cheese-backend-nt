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
import org.rucca.cheese.model.ProjectsGet200ResponseDTO
import org.rucca.cheese.model.ProjectsPost200ResponseDTO
import org.rucca.cheese.model.ProjectsPostRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface ProjectsApi {

    @Operation(
        tags = ["default"],
        summary = "List Projects",
        operationId = "projectsGet",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [
                            Content(
                                schema = Schema(implementation = ProjectsGet200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/projects"],
        produces = ["application/json"],
    )
    fun projectsGet(
        @Parameter(description = "父项目ID (可选)")
        @Valid
        @RequestParam(value = "parent_id", required = false)
        parentId: kotlin.Long?,
        @Parameter(description = "负责人ID (可选)")
        @Valid
        @RequestParam(value = "leader_id", required = false)
        leaderId: kotlin.Long?,
        @Parameter(description = "成员ID (可选)")
        @Valid
        @RequestParam(value = "member_id", required = false)
        memberId: kotlin.Long?,
        @Parameter(description = "状态 (可选)")
        @Valid
        @RequestParam(value = "status", required = false)
        status: kotlin.String?,
        @Parameter(description = "起始ID")
        @Valid
        @RequestParam(value = "page_start", required = false)
        pageStart: kotlin.Long?,
        @Parameter(description = "每页数量 (默认20)", schema = Schema(defaultValue = "20"))
        @Valid
        @RequestParam(value = "page_size", required = false, defaultValue = "20")
        pageSize: kotlin.Int,
    ): ResponseEntity<ProjectsGet200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

    @Operation(
        tags = ["default"],
        summary = "Create Project",
        operationId = "projectsPost",
        description = """""",
        responses =
            [
                ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content =
                        [
                            Content(
                                schema = Schema(implementation = ProjectsPost200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/projects"],
        produces = ["application/json"],
        consumes = ["application/json"],
    )
    fun projectsPost(
        @Parameter(description = "", required = true)
        @Valid
        @RequestBody
        projectsPostRequestDTO: ProjectsPostRequestDTO
    ): ResponseEntity<ProjectsPost200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
