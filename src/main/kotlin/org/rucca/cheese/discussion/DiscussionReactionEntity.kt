package org.rucca.cheese.discussion

import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DiscussionReactionRepository : CursorPagingRepository<DiscussionReaction, IdType> {
    /** 查找与特定讨论相关的所有反应 */
    @Query(
        "SELECT r FROM DiscussionReaction r WHERE r.projectDiscussion.id = :discussionId AND r.deletedAt IS NULL"
    )
    fun findAllByDiscussionId(discussionId: IdType): List<DiscussionReaction>
}
