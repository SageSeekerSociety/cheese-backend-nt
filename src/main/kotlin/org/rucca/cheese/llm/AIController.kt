package org.rucca.cheese.llm

import org.rucca.cheese.api.AiApi
import org.rucca.cheese.auth.AuthenticationService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.llm.service.UserAIQuotaService
import org.rucca.cheese.model.GetUserAiQuota200ResponseDTO
import org.rucca.cheese.model.QuotaInfoDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AIController(
    private val userAIQuotaService: UserAIQuotaService,
    private val authenticationService: AuthenticationService,
) : AiApi {
    @Guard("query", "ai:quota")
    override fun getUserAiQuota(): ResponseEntity<GetUserAiQuota200ResponseDTO> {
        val userId = authenticationService.getCurrentUserId()
        val quota = userAIQuotaService.getUserQuota(userId)

        return ResponseEntity.ok(
            GetUserAiQuota200ResponseDTO(
                200,
                QuotaInfoDTO(
                    remaining = quota.remainingQuota,
                    total = quota.dailyQuota,
                    resetTime = userAIQuotaService.getUserResetTime(),
                ),
                "OK",
            )
        )
    }
}
