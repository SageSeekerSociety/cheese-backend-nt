package org.rucca.cheese.llm.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import java.util.Optional
import org.rucca.cheese.llm.model.UserAIQuota
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository

@Repository
interface UserAIQuotaRepository : JpaRepository<UserAIQuota, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT q FROM UserAIQuota q WHERE q.userId = :userId")
    fun findByUserIdWithLock(userId: Long): Optional<UserAIQuota>

    fun findByUserId(userId: Long): Optional<UserAIQuota>
}
