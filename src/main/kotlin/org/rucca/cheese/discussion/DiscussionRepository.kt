package org.rucca.cheese.discussion

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DiscussionRepository : CursorPagingRepository<Discussion, IdType> {
    /** 查找与指定父讨论ID关联的所有讨论 */
    @Query("SELECT d FROM Discussion d WHERE d.parent.id = :parentId AND d.deletedAt IS NULL")
    fun findAllByParentId(parentId: IdType): List<Discussion>

    /** 按项目ID查找讨论 */
    @Query("SELECT d FROM Discussion d WHERE d.project.id = :projectId AND d.deletedAt IS NULL")
    fun findByProjectId(projectId: IdType): List<Discussion>
}
