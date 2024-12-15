package org.rucca.cheese.project

import Project
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where


/**
 * @author Qhbee
 * @version 1.0 2024/12/15 下午3:18
 */
@Entity
@Table(name = "project_discussion")
@SQLDelete(sql = "UPDATE project_discussion SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
class ProjectDiscussion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private val project: Project? = null

    @Column(columnDefinition = "jsonb", nullable = false)
    private val content: String? = null // ProjectContent的JSON格式

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private val sender: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private val parent: ProjectDiscussion? = null

    @Column(name = "created_at", nullable = false)
    private val createdAt: Long? = null

    @Column(name = "updated_at")
    private val updatedAt: Long? = null

    @Column(name = "deleted_at")
    private val deletedAt: Long? = null
}