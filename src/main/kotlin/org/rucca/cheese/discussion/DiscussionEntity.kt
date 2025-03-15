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
import org.springframework.data.jpa.repository.Query
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
    var modelType: DiscussableModelType? = null,
    @Column(name = "model_id", nullable = false) var modelId: IdType? = null,
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

@Repository
interface DiscussionRepository : CursorPagingRepository<Discussion, IdType> {
    /** 查找与指定父讨论ID关联的所有讨论 */
    @Query("SELECT d FROM Discussion d WHERE d.parent.id = :parentId AND d.deletedAt IS NULL")
    fun findAllByParentId(parentId: IdType): List<Discussion>

    /** 按模型类型和模型ID查找讨论 */
    @Query(
        "SELECT d FROM Discussion d WHERE d.modelType = :modelType AND d.modelId = :modelId AND d.deletedAt IS NULL"
    )
    fun findByModelTypeAndModelId(
        discussableModelType: DiscussableModelType,
        modelId: IdType,
    ): List<Discussion>
}
