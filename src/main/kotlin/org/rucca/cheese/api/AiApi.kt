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
import org.rucca.cheese.model.GetUserAiQuota200ResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
interface AIApi {

    @Operation(
        tags = ["AI"],
        summary = "Get Current User's AI Quota",
        operationId = "getUserAiQuota",
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
                                    Schema(implementation = GetUserAiQuota200ResponseDTO::class)
                            )
                        ],
                )
            ],
        security = [SecurityRequirement(name = "BearerAuth")],
    )
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["/ai/quota"],
        produces = ["application/json"],
    )
    suspend fun getUserAiQuota(): ResponseEntity<GetUserAiQuota200ResponseDTO> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
