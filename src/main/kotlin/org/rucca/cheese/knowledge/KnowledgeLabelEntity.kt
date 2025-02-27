package org.rucca.cheese.knowledge

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.project.Knowledge
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(indexes = [Index(columnList = "knowledge_id"), Index(columnList = "label")])
class KnowledgeLabelEntity(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var knowledge: Knowledge? = null,
    @Column(nullable = false, length = 50) var label: String? = null,
) : BaseEntity()

interface KnowledgeLabelRepository : JpaRepository<KnowledgeLabelEntity, IdType>
