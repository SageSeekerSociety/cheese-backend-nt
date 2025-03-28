package org.rucca.cheese.project.models

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "project_membership",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "user_id"])],
    indexes =
        [
            Index(columnList = "project_id"),
            Index(columnList = "user_id"),
            Index(columnList = "role"),
        ],
)
class ProjectMembership(
    @JoinColumn(name = "project_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project,
    @Column(name = "user_id", nullable = false) var userId: IdType,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var role: ProjectMemberRole,
    @Column(nullable = true, columnDefinition = "text") var notes: String? = null,
) : BaseEntity()
