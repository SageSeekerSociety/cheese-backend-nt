package org.rucca.cheese.task

import java.time.LocalDateTime
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.ApproveType
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.Query

interface TaskRepository : CursorPagingRepository<Task, IdType> {
    fun existsByCategoryId(categoryId: IdType): Boolean

    fun findByCategoryId(categoryId: IdType): List<Task>

    fun findBySpaceId(spaceId: IdType): List<Task>

    @Query(
        """
        SELECT task
        FROM Task task
        WHERE task.space.id = :spaceId
          AND task.createdAt >= COALESCE(:from, task.createdAt)
          AND task.createdAt <= COALESCE(:to, task.createdAt)
          AND task.category.id = COALESCE(:categoryId, task.category.id)
          AND task.creator.id = COALESCE(:publisherId, task.creator.id)
          AND task.approved = COALESCE(:approved, task.approved)
        """
    )
    fun findAnalyticsTasks(
        spaceId: IdType,
        from: LocalDateTime?,
        to: LocalDateTime?,
        categoryId: IdType?,
        publisherId: IdType?,
        approved: ApproveType?,
    ): List<Task>
}
