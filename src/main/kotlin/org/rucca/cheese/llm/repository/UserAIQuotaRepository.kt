package org.rucca.cheese.llm.repository

import org.rucca.cheese.llm.model.UserAIQuota
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface UserAIQuotaRepository : JpaRepository<UserAIQuota, Long>
