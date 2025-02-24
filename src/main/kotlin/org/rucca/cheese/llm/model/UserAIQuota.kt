package org.rucca.cheese.llm.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_ai_quota")
class UserAIQuota(
    @Id @Column(name = "user_id") val userId: Long,
    @Column(name = "daily_quota") var dailyQuota: Int,
    @Column(name = "remaining_quota") var remainingQuota: Int,
    @Column(name = "last_reset_time") var lastResetTime: LocalDateTime,
    @Column(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at") var updatedAt: LocalDateTime = LocalDateTime.now(),
)
