package org.rucca.cheese.knowledge

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "knowledge_label",
    indexes = [Index(columnList = "knowledge_id"), Index(columnList = "label")],
)
class KnowledgeLabelEntity(
    @JoinColumn(nullable = false, name = "knowledge_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var knowledge: Knowledge? = null,
    @Column(nullable = false, length = 50) var label: String? = null,
) : BaseEntity()

interface KnowledgeLabelRepository : JpaRepository<KnowledgeLabelEntity, IdType> {
    @Query(
        "SELECT kl FROM KnowledgeLabelEntity kl WHERE kl.knowledge.team.id = :teamId GROUP BY kl.label, kl.id ORDER BY COUNT(kl.id) DESC"
    )
    fun findPopularLabelsByTeamId(teamId: Long): List<KnowledgeLabelEntity>

    @Query(
        "SELECT DISTINCT kl.label FROM KnowledgeLabelEntity kl WHERE kl.knowledge.team.id = :teamId"
    )
    fun findUniqueLabelsForTeam(teamId: Long): List<String>
}
