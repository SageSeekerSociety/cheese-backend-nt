package org.rucca.cheese.discussion

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "project_discussion_id"), Index(columnList = "user_id")])
class DiscussionReaction(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var projectDiscussion: Discussion? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var user: User? = null,
    @Column(nullable = false) var emoji: String? = null,
) : BaseEntity()

interface DiscussionReactionRepository : JpaRepository<DiscussionReaction, IdType> {
    fun findAllByProjectDiscussionId(discussionId: IdType): List<DiscussionReaction>
}
