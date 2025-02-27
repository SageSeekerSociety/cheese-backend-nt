package org.rucca.cheese.task

import java.util.Optional
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskAIAdviceRepository : JpaRepository<TaskAIAdvice, IdType> {
    fun findByTaskId(taskId: IdType): Optional<TaskAIAdvice>

    fun findByTaskIdAndModelHash(taskId: IdType, modelHash: String): Optional<TaskAIAdvice>
}
