package org.rucca.cheese.space

import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import org.rucca.cheese.common.persistent.BaseEntity
import org.rucca.cheese.common.persistent.IdType
import org.rucca.cheese.user.Avatar
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    indexes =
        [
            Index(columnList = "name"),
        ]
)
class Space(
    @Column(nullable = false) var name: String? = null,
    @Column(nullable = false) var intro: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var description: String? = null,
    @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var avatar: Avatar? = null,
    @Column(nullable = false) var enableRank: Boolean? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var announcements: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT") var taskTemplates: String? = null,
) : BaseEntity()

interface SpaceRepository : JpaRepository<Space, IdType> {
    fun existsByName(name: String): Boolean
}
