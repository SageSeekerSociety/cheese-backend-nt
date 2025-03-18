package org.rucca.cheese.discussion

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "discussion_reaction",
    indexes = [Index(columnList = "discussion_id"), Index(columnList = "user_id")],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "uk_discussion_reaction_user_emoji",
                columnNames = ["discussion_id", "user_id", "emoji"],
            )
        ],
)
class DiscussionReaction(
    @JoinColumn(name = "discussion_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var discussion: Discussion? = null,
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var user: User? = null,
    @Column(nullable = false) var emoji: String? = null,
) : BaseEntity()

@Repository
interface DiscussionReactionRepository : CursorPagingRepository<DiscussionReaction, IdType> {
    /** 查找与特定讨论相关的所有反应 */
    @Query(
        "SELECT r FROM DiscussionReaction r WHERE r.discussion.id = :discussionId AND r.deletedAt IS NULL"
    )
    fun findAllByDiscussionId(discussionId: IdType): List<DiscussionReaction>
}
