package org.rucca.cheese.team

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
                ])
class Team(
        @Column(nullable = false) var name: String? = null,
        @Column(nullable = false) var description: String? = null,
        @JoinColumn(nullable = false) @ManyToOne(fetch = FetchType.LAZY) var avatar: Avatar? = null,
) : BaseEntity()

interface TeamRepository : JpaRepository<Team, IdType> {
    fun existsByName(name: String): Boolean
}
