package org.rucca.cheese.project

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.User
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "project_id"),
            Index(columnList = "sender_id"),
            Index(columnList = "parent_id"),
        ]
)
class ProjectDiscussion(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var project: Project? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var sender: User? = null,
    @JoinColumn(nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    var parent: ProjectDiscussion? = null,
    @Column(nullable = false) var content: String? = null,
    @ElementCollection var mentionedUserIds: Set<IdType> = HashSet(),
) : BaseEntity()

interface ProjectDiscussionRepository : JpaRepository<ProjectDiscussion, IdType> {}
