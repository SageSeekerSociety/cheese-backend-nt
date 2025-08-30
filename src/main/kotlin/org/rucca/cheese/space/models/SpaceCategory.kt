package org.rucca.cheese.space.models

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity

@Entity
@Table(
    name = "space_categories",
    uniqueConstraints = [UniqueConstraint(columnNames = ["space_id", "name"])],
    indexes = [Index(name = "idx_spacecategories_archived", columnList = "archived_at")],
)
@SQLRestriction("deleted_at IS NULL")
class SpaceCategory(
    @Column(nullable = false) var name: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    val space: Space,
    @Column(nullable = true, columnDefinition = "TEXT") var description: String? = null,
    @Column(name = "display_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    var displayOrder: Int = 0,
    @Column(name = "archived_at", nullable = true) var archivedAt: OffsetDateTime? = null,
) : BaseEntity() {
    @get:Transient
    val isArchived: Boolean
        get() = archivedAt != null
}
