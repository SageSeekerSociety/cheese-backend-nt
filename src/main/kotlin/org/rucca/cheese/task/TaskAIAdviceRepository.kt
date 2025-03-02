package org.rucca.cheese.task

import java.util.Optional
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TaskAIAdviceRepository : JpaRepository<TaskAIAdvice, IdType> {
    fun findByTaskId(taskId: IdType): Optional<TaskAIAdvice>

    fun findByTaskIdAndModelHash(taskId: IdType, modelHash: String): TaskAIAdvice?

    @Modifying
    @Transactional
    @Query(
        "UPDATE TaskAIAdvice t SET t.status = :status WHERE t.id = :id AND t.modelHash = :modelHash"
    )
    fun updateStatusByIdAndModelHash(id: IdType, modelHash: String, status: TaskAIAdviceStatus): Int
}
