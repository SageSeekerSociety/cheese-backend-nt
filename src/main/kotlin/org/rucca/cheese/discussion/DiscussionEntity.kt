package org.rucca.cheese.discussion

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.project.Project
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
class Discussion(
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var sender: User? = null,
    @JoinColumn(nullable = true) @ManyToOne(fetch = FetchType.LAZY) var parent: Discussion? = null,
    @Column(nullable = false) var content: String? = null,
    @ElementCollection var mentionedUserIds: Set<IdType> = HashSet(),
    @JoinColumn(nullable = true) @ManyToOne(fetch = FetchType.LAZY) var project: Project? = null,
) : BaseEntity()

interface DiscussionRepository : JpaRepository<Discussion, IdType> {
    fun findAllByProjectId(projectIds: IdType): List<Discussion>
}
