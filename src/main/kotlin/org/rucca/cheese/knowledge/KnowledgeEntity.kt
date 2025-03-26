package org.rucca.cheese.knowledge

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.Type
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.discussion.Discussion
import org.rucca.cheese.material.Material
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.Query

enum class KnowledgeType {
    MATERIAL,
    LINK,
    TEXT,
    CODE,
}

enum class KnowledgeSource {
    MANUAL, // 手动添加
    FROM_DISCUSSION, // 从讨论中添加
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "name"),
            Index(columnList = "team_id"),
            Index(columnList = "source_type"),
            Index(columnList = "project_id"),
            Index(columnList = "discussion_id"),
        ]
)
class Knowledge(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false, columnDefinition = "text") var description: String? = null,
    @Column(nullable = false) @Enumerated(EnumType.STRING) var type: KnowledgeType? = null,
    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb", nullable = false)
    var content: String? = null,

    // 关联到Team
    @JoinColumn(name = "team_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var team: Team? = null,

    // 关联到Material（可选）
    @JoinColumn(nullable = true) @ManyToOne(fetch = FetchType.LAZY) var material: Material? = null,

    // 创建者
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var createdBy: User? = null,

    // 知识来源类型
    @Column(name = "source_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var sourceType: KnowledgeSource = KnowledgeSource.MANUAL,

    // 来源项目ID（可选）
    @Column(name = "project_id", nullable = true) var projectId: Long? = null,

    // 来源讨论ID（可选，如果从讨论中添加）
    @JoinColumn(name = "discussion_id", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    var sourceDiscussion: Discussion? = null,

    // 标签
    @OneToMany(
        targetEntity = KnowledgeLabelEntity::class,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    var knowledgeLabels: MutableList<KnowledgeLabelEntity> = mutableListOf(),
) : BaseEntity()

interface KnowledgeRepository : CursorPagingRepository<Knowledge, IdType> {
    // 通过团队ID查找知识条目
    fun findByTeamId(teamId: Long): List<Knowledge>

    // 通过项目ID查找知识条目
    fun findByProjectId(projectId: Long): List<Knowledge>

    // 通过团队ID和项目ID查找知识条目
    @Query(
        "SELECT k FROM Knowledge k WHERE k.team.id = :teamId AND (k.projectId = :projectId OR :projectId IS NULL)"
    )
    fun findByTeamIdAndProjectId(teamId: Long, projectId: Long?): List<Knowledge>

    // 通过团队ID和标签查找知识条目
    @Query(
        "SELECT DISTINCT k FROM Knowledge k JOIN k.knowledgeLabels kl WHERE k.team.id = :teamId AND kl.label IN :labels"
    )
    fun findByTeamIdAndLabels(teamId: Long, labels: List<String>): List<Knowledge>
}
