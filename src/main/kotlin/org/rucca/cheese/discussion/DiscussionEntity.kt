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
import org.rucca.cheese.project.Project
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

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

@Repository
interface DiscussionRepository : CursorPagingRepository<Discussion, IdType> {
    /** 查找与指定父讨论ID关联的所有讨论 */
    @Query("SELECT d FROM Discussion d WHERE d.parent.id = :parentId AND d.deletedAt IS NULL")
    fun findAllByParentId(parentId: IdType): List<Discussion>

    /** 按项目ID查找讨论 */
    @Query("SELECT d FROM Discussion d WHERE d.project.id = :projectId AND d.deletedAt IS NULL")
    fun findByProjectId(projectId: IdType): List<Discussion>
}
