/*
 *  Description: This file defines the Discussion entity and its repository.
 *               It stores the information of discussions.
 */

package org.rucca.cheese.discussion

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.project.Project
import org.rucca.cheese.user.User

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "project_id"), Index(columnList = "sender_id")])
class Discussion(
    @JoinColumn(name = "project_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project? = null,
    @JoinColumn(name = "sender_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var sender: User? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var content: String? = null,
    @JoinColumn(name = "parent_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var parent: Discussion? = null,
    @ElementCollection
    @CollectionTable(
        name = "discussion_mentioned_users",
        joinColumns = [JoinColumn(name = "discussion_id")],
    )
    @Column(name = "user_id")
    var mentionedUserIds: Set<IdType> = emptySet(),
) : BaseEntity()

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "discussion_reaction",
    indexes = [Index(columnList = "project_discussion_id"), Index(columnList = "user_id")],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "uk_discussion_reaction_user_emoji",
                columnNames = ["project_discussion_id", "user_id", "emoji"],
            )
        ],
)
class DiscussionReaction(
    @JoinColumn(name = "project_discussion_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var projectDiscussion: Discussion? = null,
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var user: User? = null,
    @Column(nullable = false) var emoji: String? = null,
) : BaseEntity()
