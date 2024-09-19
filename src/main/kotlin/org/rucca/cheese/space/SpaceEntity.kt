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
                        Index(columnList = "name", unique = true),
                ])
class Space(
        @Column(nullable = false) var name: String?,
        @Column(nullable = false) var description: String?,
        @ManyToOne(fetch = FetchType.LAZY) var avatar: Avatar?,
) : BaseEntity()

interface SpaceRepository : JpaRepository<Space, IdType> {
    fun existsByName(name: String): Boolean
}
