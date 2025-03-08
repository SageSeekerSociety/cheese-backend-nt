package org.rucca.cheese.project

import jakarta.persistence.*
import java.time.LocalDateTime
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.pagination.repository.CursorPagingRepository
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.team.Team
import org.rucca.cheese.user.User

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "name"),
            Index(columnList = "team_id"),
            Index(columnList = "leader_id"),
            Index(columnList = "parent_id"),
        ]
)
class Project(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false, columnDefinition = "text") var description: String? = null,
    @Column(nullable = false, columnDefinition = "text") var content: String? = null,
    @Column(nullable = false, length = 7) var colorCode: String? = null,
    @Column(nullable = false) var startDate: LocalDateTime? = null,
    @Column(nullable = false) var endDate: LocalDateTime? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var team: Team? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var leader: User? = null,
    @JoinColumn(nullable = true) @ManyToOne(fetch = FetchType.LAZY) var parent: Project? = null,
    @Column(nullable = true) var externalTaskId: IdType? = null,
    @Column(nullable = true) var githubRepo: String? = null,
) : BaseEntity()

interface ProjectRepository : CursorPagingRepository<Project, IdType> {}
