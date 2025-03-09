package org.rucca.cheese.llm.repository

import java.math.BigDecimal
import java.time.LocalDateTime
import org.rucca.cheese.llm.model.UserSeuConsumption
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserSeuConsumptionRepository : JpaRepository<UserSeuConsumption, Long> {

    fun findByCacheKeyAndCacheExpireAtAfter(
        cacheKey: String,
        now: LocalDateTime,
    ): UserSeuConsumption?

    @Query(
        """
        SELECT COALESCE(SUM(c.seuConsumed), 0)
        FROM UserSeuConsumption c
        WHERE c.userId = :userId
        AND c.createdAt >= :startTime
        AND c.createdAt < :endTime
    """
    )
    fun sumSeuConsumedByUserIdAndTimeRange(
        userId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): BigDecimal
}
