package org.rucca.cheese.project

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.material.Material
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

enum class KnowledgeType {
    DOCUMENT,
    LINK,
    TEXT,
    IMAGE,
}

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "name")])
class Knowledge(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false, columnDefinition = "text") var description: String? = null,
    @Column(nullable = false) @Enumerated(EnumType.STRING) var type: KnowledgeType? = null,
    @Column(columnDefinition = "jsonb", nullable = false) var content: String? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var material: Material? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var createdBy: User? = null,
    @ElementCollection
    @CollectionTable(
        name = "project_knowledge_project",
        joinColumns = [JoinColumn(name = "knowledge_id")],
    )
    var projectIds: Set<Long> = HashSet(),
) : BaseEntity()

interface KnowledgeRepository : JpaRepository<Knowledge, IdType> {}
