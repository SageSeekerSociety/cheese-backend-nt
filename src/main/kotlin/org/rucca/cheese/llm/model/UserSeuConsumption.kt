package org.rucca.cheese.llm.model

import jakarta.persistence.*
import java.math.BigDecimal
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity

/** 用户SEU消费记录 */
@Entity
@Table(name = "user_seu_consumption")
@SQLRestriction("deleted_at IS NULL")
class UserSeuConsumption(
    @Column(name = "user_id") val userId: Long,
    @Column(name = "request_id") val requestId: String,
    @Enumerated(EnumType.STRING) @Column(name = "resource_type") val resourceType: AIResourceType,
    @Column(name = "tokens_used") val tokensUsed: Int,
    @Column(name = "seu_consumed", precision = 10, scale = 4) val seuConsumed: BigDecimal,
    @Column(name = "is_cached") val isCached: Boolean = false,
    @Column(name = "cache_key") val cacheKey: String? = null,
    @Column(name = "cache_expire_at") val cacheExpireAt: java.time.LocalDateTime? = null,
) : BaseEntity()
