/*
 *  Description: This file defines the Discussion entity and its repository.
 *               It stores the information of discussions.
 */

package org.rucca.cheese.discussion

import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.stereotype.Repository

/** 模型类型枚举 */
enum class DiscussableModelType {
    PROJECT
}

@DynamicUpdate
@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "model_type, model_id"), Index(columnList = "sender_id")])
class Discussion(
    @Column(name = "model_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var modelType: DiscussableModelType,
    @Column(name = "model_id", nullable = false) var modelId: IdType,
    @JoinColumn(name = "sender_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var sender: User,
    @Column(nullable = false, columnDefinition = "TEXT") var content: String,
    @JoinColumn(name = "parent_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var parent: Discussion? = null,
    @ElementCollection
    @CollectionTable(
        name = "discussion_mentioned_users",
        joinColumns = [JoinColumn(name = "discussion_id")],
    )
    @Column(name = "user_id")
    var mentionedUserIds: Set<IdType>,
) : BaseEntity()

@Repository interface DiscussionRepository : CursorPagingRepository<Discussion, IdType> {}
