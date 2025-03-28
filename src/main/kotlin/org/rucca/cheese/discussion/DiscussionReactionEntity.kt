package org.rucca.cheese.discussion

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "discussion_reaction",
    indexes =
        [
            Index(
                name = "idx_reaction_discussion_type",
                columnList = "discussion_id, reaction_type_id",
            ),
            Index(name = "idx_reaction_discussion_user", columnList = "discussion_id, user_id"),
        ],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "uk_discussion_reaction_user_type",
                columnNames = ["discussion_id", "user_id", "reaction_type_id"],
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
    @JoinColumn(name = "reaction_type_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var reactionType: ReactionType? = null,
) : BaseEntity()

/** Data class representing reaction type counts */
data class ReactionTypeCount(val reactionType: ReactionType, val count: Long)

@Repository
interface DiscussionReactionRepository : CursorPagingRepository<DiscussionReaction, IdType> {
    /**
     * Count reactions grouped by type for a specific discussion
     *
     * @return List of reaction counts by type
     */
    @Query(
        "SELECT new org.rucca.cheese.discussion.ReactionTypeCount(r.reactionType, COUNT(r)) " +
            "FROM DiscussionReaction r " +
            "WHERE r.discussion.id = :discussionId " +
            "GROUP BY r.reactionType"
    )
    fun countReactionsByDiscussionId(discussionId: IdType): List<ReactionTypeCount>

    fun countReactionByDiscussionIdAndReactionTypeId(
        discussionId: IdType,
        reactionTypeId: IdType,
    ): Long

    /** Find users who reacted with a specific reaction type to a discussion */
    @Query(
        "SELECT r.user FROM DiscussionReaction r " +
            "WHERE r.discussion.id = :discussionId AND r.reactionType.id = :reactionTypeId"
    )
    fun findUsersByDiscussionIdAndReactionTypeId(
        discussionId: IdType,
        reactionTypeId: IdType,
    ): List<User>

    /** Check if a user has reacted with a specific reaction type to a discussion */
    fun existsByDiscussionIdAndUserIdAndReactionTypeId(
        discussionId: IdType,
        userId: IdType,
        reactionTypeId: IdType,
    ): Boolean

    /** Delete a reaction */
    @Modifying
    @Query(
        "DELETE FROM DiscussionReaction r WHERE " +
            "r.discussion.id = :discussionId AND r.user.id = :userId AND r.reactionType.id = :reactionTypeId"
    )
    fun deleteReaction(discussionId: IdType, userId: IdType, reactionTypeId: IdType): Int
}
