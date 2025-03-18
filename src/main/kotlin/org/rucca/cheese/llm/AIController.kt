package org.rucca.cheese.llm

import java.math.RoundingMode
import org.rucca.cheese.api.AiApi
import org.rucca.cheese.auth.JwtService
import org.rucca.cheese.auth.annotation.Guard
import org.rucca.cheese.auth.spring.UseOldAuth
import org.rucca.cheese.llm.service.UserQuotaService
import org.rucca.cheese.model.GetUserAiQuota200ResponseDTO
import org.rucca.cheese.model.QuotaInfoDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
@UseOldAuth
class AIController(
    private val userQuotaService: UserQuotaService,
    private val jwtService: JwtService,
) : AiApi {
    @Guard("query", "ai:quota")
    override fun getUserAiQuota(): ResponseEntity<GetUserAiQuota200ResponseDTO> {
        val userId = jwtService.getCurrentUserId()
        val quota = userQuotaService.getUserQuota(userId)

        return ResponseEntity.ok(
            GetUserAiQuota200ResponseDTO(
                200,
                QuotaInfoDTO(
                    remaining = quota.remainingSeu.setScale(2, RoundingMode.HALF_UP).toDouble(),
                    total = quota.dailySeuQuota.setScale(2, RoundingMode.HALF_UP).toDouble(),
                    resetTime = userQuotaService.getUserResetTime(),
                ),
                "OK",
            )
        )
    }
}
