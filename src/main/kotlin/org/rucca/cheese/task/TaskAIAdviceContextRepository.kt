package org.rucca.cheese.task

import java.util.Optional
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskAIAdviceContextRepository : JpaRepository<TaskAIAdviceContext, IdType> {
    /** 按任务ID查找上下文 */
    fun findByTaskId(taskId: IdType): List<TaskAIAdviceContext>

    /** 按任务ID和部分查找上下文 */
    fun findByTaskIdAndSection(taskId: IdType, section: String): List<TaskAIAdviceContext>

    /** 按任务ID、部分和索引查找特定上下文 */
    fun findByTaskIdAndSectionAndSectionIndex(
        taskId: IdType,
        section: String?,
        sectionIndex: Int?,
    ): Optional<TaskAIAdviceContext>
}
