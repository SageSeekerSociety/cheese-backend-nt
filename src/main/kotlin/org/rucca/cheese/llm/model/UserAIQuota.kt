package org.rucca.cheese.llm.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity

/** 用户AI配额 */
@Entity
@Table(name = "user_ai_quota")
@SQLRestriction("deleted_at IS NULL")
class UserAIQuota(
    @Column(name = "user_id") val userId: Long,
    @Column(name = "daily_seu_quota", precision = 10, scale = 4) var dailySeuQuota: BigDecimal,
    @Column(name = "remaining_seu", precision = 10, scale = 4) var remainingSeu: BigDecimal,
    @Column(name = "total_seu_consumed", precision = 10, scale = 4)
    var totalSeuConsumed: BigDecimal = BigDecimal.ZERO,
    @Column(name = "last_reset_time") var lastResetTime: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
